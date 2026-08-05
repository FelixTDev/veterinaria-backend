package com.veterinaria.backend.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.veterinaria.backend.auth.exception.PasswordPolicyException;
import com.veterinaria.backend.auth.service.PasswordPolicyValidator;
import com.veterinaria.backend.usuario.dto.ActualizarRolesUsuarioRequest;
import com.veterinaria.backend.usuario.dto.ActualizarUsuarioRequest;
import com.veterinaria.backend.usuario.dto.CambiarEstadoUsuarioRequest;
import com.veterinaria.backend.usuario.dto.CrearUsuarioRequest;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.dto.UsuarioDetalleResponse;
import com.veterinaria.backend.usuario.dto.UsuarioResumenResponse;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.exception.CorreoDuplicadoException;
import com.veterinaria.backend.usuario.exception.OperacionAdministradorException;
import com.veterinaria.backend.usuario.exception.RolInvalidoException;
import com.veterinaria.backend.usuario.exception.UsuarioNoEncontradoException;
import com.veterinaria.backend.usuario.mapper.UsuarioMapper;
import com.veterinaria.backend.usuario.repository.RolRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioGestionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioRolRepository usuarioRolRepository;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    private PasswordEncoder passwordEncoder;
    private UsuarioGestionService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new UsuarioGestionService(
                usuarioRepository,
                rolRepository,
                usuarioRolRepository,
                passwordEncoder,
                passwordPolicyValidator,
                new UsuarioMapper());
    }

    @Test
    void shouldCreateUsuarioNormalizeEmailEncryptPasswordAndAssignRoles() {
        CrearUsuarioRequest request = crearRequest(" Ana.Torres@TEST.Dev ", "RECEPCIONISTA", "PELUQUERO");
        when(usuarioRepository.existsByCorreoIgnoreCase("ana.torres@test.dev")).thenReturn(false);
        when(rolRepository.findAllByNombreIn(List.of(NombreRol.RECEPCIONISTA, NombreRol.PELUQUERO)))
                .thenReturn(List.of(rol(2L, NombreRol.RECEPCIONISTA), rol(4L, NombreRol.PELUQUERO)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(10L);
            return usuario;
        });
        UsuarioDetalleResponse response = service.crear(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario saved = usuarioCaptor.getValue();
        assertThat(saved.getCorreo()).isEqualTo("ana.torres@test.dev");
        assertThat(saved.getActivo()).isTrue();
        assertThat(saved.getIntentosFallidos()).isZero();
        assertThat(saved.getBloqueadoHasta()).isNull();
        assertThat(passwordEncoder.matches("Password123*", saved.getPasswordHash())).isTrue();
        assertThat(response.roles()).containsExactly("RECEPCIONISTA", "PELUQUERO");
        verify(passwordPolicyValidator).validate("Password123*");
        verify(usuarioRolRepository).saveAll(any());
    }

    @Test
    void shouldRejectDuplicateEmailOnCreate() {
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@test.dev")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(crearRequest("ANA@test.dev", "RECEPCIONISTA")))
                .isInstanceOf(CorreoDuplicadoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void shouldRejectWeakPasswordOnCreate() {
        CrearUsuarioRequest request = crearRequest("ana@test.dev", "RECEPCIONISTA");
        org.mockito.Mockito.doThrow(new PasswordPolicyException("debil"))
                .when(passwordPolicyValidator).validate("Password123*");
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@test.dev")).thenReturn(false);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(PasswordPolicyException.class);
    }

    @Test
    void shouldRejectMissingOrDuplicatedRoles() {
        assertThatThrownBy(() -> service.crear(crearRequest("ana@test.dev")))
                .isInstanceOf(RolInvalidoException.class);

        assertThatThrownBy(() -> service.crear(crearRequest("ana@test.dev", "PELUQUERO", "peluquero")))
                .isInstanceOf(RolInvalidoException.class);
    }

    @Test
    void shouldRejectNonExistingRole() {
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@test.dev")).thenReturn(false);

        assertThatThrownBy(() -> service.crear(crearRequest("ana@test.dev", "NO_EXISTE")))
                .isInstanceOf(RolInvalidoException.class);
    }

    @Test
    void shouldUpdatePersonalDataPreservingPasswordAndNormalizeEmail() {
        Usuario usuario = usuario(7L, "viejo@test.dev", true);
        String originalHash = usuario.getPasswordHash();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreoIgnoreCaseAndIdNot("nuevo@test.dev", 7L)).thenReturn(false);
        when(usuarioRolRepository.findActivosByUsuarioIds(List.of(7L)))
                .thenReturn(List.of(usuarioRol(7L, rol(2L, NombreRol.RECEPCIONISTA))));

        UsuarioDetalleResponse response = service.actualizar(7L,
                new ActualizarUsuarioRequest("Nuevo", null, "Apellido", null, " NUEVO@Test.Dev ", "999111222"));

        assertThat(usuario.getCorreo()).isEqualTo("nuevo@test.dev");
        assertThat(usuario.getPasswordHash()).isEqualTo(originalHash);
        assertThat(response.nombreCompleto()).isEqualTo("Nuevo Apellido");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void shouldRejectMissingUsuarioOnUpdate() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L,
                new ActualizarUsuarioRequest("Ana", null, "Torres", null, "ana@test.dev", null)))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void shouldDeactivateUsuarioButRejectSelfAndLastAdministrator() {
        Usuario usuario = usuario(2L, "admin@test.dev", true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(2L, NombreRol.ADMINISTRADOR)).thenReturn(true);
        when(usuarioRolRepository.countActiveAdministradores()).thenReturn(2L);
        when(usuarioRolRepository.findActivosByUsuarioIds(List.of(2L)))
                .thenReturn(List.of(usuarioRol(2L, rol(1L, NombreRol.ADMINISTRADOR))));

        UsuarioDetalleResponse response = service.cambiarEstado(2L, new CambiarEstadoUsuarioRequest(false), 1L);

        assertThat(response.activo()).isFalse();
        verify(usuarioRepository).save(usuario);

        assertThatThrownBy(() -> service.cambiarEstado(1L, new CambiarEstadoUsuarioRequest(false), 1L))
                .isInstanceOf(OperacionAdministradorException.class);
    }

    @Test
    void shouldRejectDeactivatingLastActiveAdministrator() {
        Usuario usuario = usuario(1L, "admin@test.dev", true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(1L, NombreRol.ADMINISTRADOR)).thenReturn(true);
        when(usuarioRolRepository.countActiveAdministradores()).thenReturn(1L);

        assertThatThrownBy(() -> service.cambiarEstado(1L, new CambiarEstadoUsuarioRequest(false), 99L))
                .isInstanceOf(OperacionAdministradorException.class);
    }

    @Test
    void shouldReplaceRolesAndRejectRemovingLastAdministrator() {
        Usuario usuario = usuario(8L, "ana@test.dev", true);
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findAllByNombreIn(List.of(NombreRol.VETERINARIO, NombreRol.PELUQUERO)))
                .thenReturn(List.of(rol(3L, NombreRol.VETERINARIO), rol(4L, NombreRol.PELUQUERO)));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(8L, NombreRol.ADMINISTRADOR)).thenReturn(false);
        assertThat(service.actualizarRoles(8L,
                new ActualizarRolesUsuarioRequest(List.of("VETERINARIO", "PELUQUERO")), 1L).roles())
                .containsExactly("VETERINARIO", "PELUQUERO");

        verify(usuarioRolRepository).deleteByUsuario_Id(8L);
        verify(usuarioRolRepository).saveAll(any());
    }

    @Test
    void shouldListAndGetDetalleWithBatchRoles() {
        Usuario usuario = usuario(5L, "ana@test.dev", true);
        when(usuarioRepository.findAllForGestion("ana", true, NombreRol.RECEPCIONISTA, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(usuario), PageRequest.of(0, 10), 1));
        when(usuarioRolRepository.findActivosByUsuarioIds(List.of(5L)))
                .thenReturn(List.of(usuarioRol(5L, rol(2L, NombreRol.RECEPCIONISTA))));

        PaginaResponse<UsuarioResumenResponse> page = service.listar(
                "ana", true, NombreRol.RECEPCIONISTA, PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).roles()).containsExactly("RECEPCIONISTA");

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        assertThat(service.obtener(5L).correo()).isEqualTo("ana@test.dev");
    }

    private CrearUsuarioRequest crearRequest(String correo, String... roles) {
        return new CrearUsuarioRequest(
                "Ana",
                null,
                "Torres",
                "Lopez",
                correo,
                "999888777",
                "Password123*",
                List.of(roles));
    }

    private Usuario usuario(Long id, String correo, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setPrimerNombre("Ana");
        usuario.setPrimerApellido("Torres");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode("Password123*"));
        usuario.setTelefono("999888777");
        usuario.setActivo(activo);
        usuario.setIntentosFallidos(0);
        return usuario;
    }

    private Rol rol(Long id, NombreRol nombreRol) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setNombre(nombreRol);
        rol.setDescripcion(nombreRol.name());
        rol.setActivo(Boolean.TRUE);
        return rol;
    }

    private UsuarioRol usuarioRol(Long usuarioId, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(usuario);
        usuarioRol.setRol(rol);
        return usuarioRol;
    }
}
