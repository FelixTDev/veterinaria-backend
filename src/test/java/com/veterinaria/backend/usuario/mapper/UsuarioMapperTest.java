package com.veterinaria.backend.usuario.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import com.veterinaria.backend.usuario.dto.RolResponse;
import com.veterinaria.backend.usuario.dto.UsuarioDetalleResponse;
import com.veterinaria.backend.usuario.dto.UsuarioResumenResponse;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;

import org.junit.jupiter.api.Test;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapper();

    @Test
    void shouldMapUsuarioToResumenWithoutSensitiveFields() {
        Usuario usuario = usuario();

        UsuarioResumenResponse response = mapper.toResumen(usuario, List.of(NombreRol.RECEPCIONISTA, NombreRol.PELUQUERO));

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.nombreCompleto()).isEqualTo("Ana Torres Lopez");
        assertThat(response.correo()).isEqualTo("ana@test.dev");
        assertThat(response.telefono()).isEqualTo("999888777");
        assertThat(response.activo()).isTrue();
        assertThat(response.roles()).containsExactly("RECEPCIONISTA", "PELUQUERO");
    }

    @Test
    void shouldMapUsuarioToDetalleWithAuditFields() {
        Usuario usuario = usuario();

        UsuarioDetalleResponse response = mapper.toDetalle(usuario, List.of(NombreRol.ADMINISTRADOR));

        assertThat(response.primerNombre()).isEqualTo("Ana");
        assertThat(response.segundoNombre()).isNull();
        assertThat(response.primerApellido()).isEqualTo("Torres");
        assertThat(response.segundoApellido()).isEqualTo("Lopez");
        assertThat(response.nombreCompleto()).isEqualTo("Ana Torres Lopez");
        assertThat(response.createdAt()).isEqualTo(usuario.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(usuario.getUpdatedAt());
        assertThat(response.ultimoAcceso()).isEqualTo(usuario.getUltimoAcceso());
        assertThat(response.roles()).containsExactly("ADMINISTRADOR");
    }

    @Test
    void shouldMapRolToResponse() {
        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre(NombreRol.ADMINISTRADOR);
        rol.setDescripcion("Acceso completo");
        rol.setActivo(Boolean.TRUE);

        RolResponse response = mapper.toRolResponse(rol);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("ADMINISTRADOR");
        assertThat(response.descripcion()).isEqualTo("Acceso completo");
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(15L);
        usuario.setPrimerNombre(" Ana ");
        usuario.setPrimerApellido("Torres");
        usuario.setSegundoApellido("Lopez");
        usuario.setCorreo("ana@test.dev");
        usuario.setTelefono("999888777");
        usuario.setPasswordHash("no-debe-salir");
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(0);
        usuario.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 0));
        usuario.setUpdatedAt(LocalDateTime.of(2026, 8, 5, 11, 0));
        usuario.setUltimoAcceso(LocalDateTime.of(2026, 8, 5, 12, 0));
        return usuario;
    }
}
