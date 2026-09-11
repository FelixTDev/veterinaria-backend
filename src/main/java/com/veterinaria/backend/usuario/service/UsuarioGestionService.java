package com.veterinaria.backend.usuario.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.veterinaria.backend.auth.service.PasswordPolicyValidator;
import com.veterinaria.backend.usuario.dto.ActualizarRolesUsuarioRequest;
import com.veterinaria.backend.usuario.dto.ActualizarUsuarioRequest;
import com.veterinaria.backend.usuario.dto.CambiarEstadoUsuarioRequest;
import com.veterinaria.backend.usuario.dto.CrearUsuarioRequest;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.dto.UsuarioDetalleResponse;
import com.veterinaria.backend.usuario.dto.UsuarioResumenResponse;
import com.veterinaria.backend.usuario.dto.UsuarioRolesResponse;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioGestionService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final UsuarioMapper usuarioMapper;

    public UsuarioGestionService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            UsuarioRolRepository usuarioRolRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional
    public UsuarioDetalleResponse crear(CrearUsuarioRequest request) {
        String correo = normalizeEmail(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException("El correo ya se encuentra registrado.");
        }
        passwordPolicyValidator.validate(request.passwordInicial());
        List<NombreRol> nombresRoles = parseAndValidateRoleNames(request.roles());
        List<Rol> roles = findActiveRoles(nombresRoles);

        Usuario usuario = new Usuario();
        usuario.setPrimerNombre(normalizeText(request.primerNombre()));
        usuario.setSegundoNombre(normalizeOptionalText(request.segundoNombre()));
        usuario.setPrimerApellido(normalizeText(request.primerApellido()));
        usuario.setSegundoApellido(normalizeOptionalText(request.segundoApellido()));
        usuario.setCorreo(correo);
        usuario.setTelefono(normalizeOptionalText(request.telefono()));
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordInicial()));
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        Usuario saved = usuarioRepository.save(usuario);
        usuarioRolRepository.saveAll(toUsuarioRoles(saved, roles));
        return usuarioMapper.toDetalle(saved, nombresRoles);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResumenResponse> listar(
            String search,
            Boolean activo,
            NombreRol rol,
            Pageable pageable) {
        validarOrden(pageable, "id", "correo", "primerNombre", "primerApellido", "activo");
        String normalizedSearch = normalizeOptionalText(search);
        Page<Usuario> usuarios = usuarioRepository.findAllForGestion(normalizedSearch, activo, rol, pageable);
        Map<Long, List<NombreRol>> rolesByUsuarioId = findRolesByUsuarioIds(
                usuarios.getContent().stream().map(Usuario::getId).toList());
        List<UsuarioResumenResponse> content = usuarios.getContent()
                .stream()
                .map(usuario -> usuarioMapper.toResumen(
                        usuario,
                        rolesByUsuarioId.getOrDefault(usuario.getId(), List.of())))
                .toList();
        return new PaginaResponse<>(
                content,
                usuarios.getNumber(),
                usuarios.getSize(),
                usuarios.getTotalElements(),
                usuarios.getTotalPages());
    }

    private void validarOrden(Pageable pageable, String... allowed) {
        java.util.Set<String> allowedSet = java.util.Set.of(allowed);
        if (pageable.getSort().stream().anyMatch(order -> !allowedSet.contains(order.getProperty()))) {
            throw new IllegalArgumentException("El orden solicitado no es valido.");
        }
    }

    @Transactional(readOnly = true)
    public UsuarioDetalleResponse obtener(Long id) {
        Usuario usuario = findUsuario(id);
        return usuarioMapper.toDetalle(usuario, rolesOf(id));
    }

    @Transactional
    public UsuarioDetalleResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = findUsuario(id);
        String correo = normalizeEmail(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw new CorreoDuplicadoException("El correo ya se encuentra registrado.");
        }
        usuario.setPrimerNombre(normalizeText(request.primerNombre()));
        usuario.setSegundoNombre(normalizeOptionalText(request.segundoNombre()));
        usuario.setPrimerApellido(normalizeText(request.primerApellido()));
        usuario.setSegundoApellido(normalizeOptionalText(request.segundoApellido()));
        usuario.setCorreo(correo);
        usuario.setTelefono(normalizeOptionalText(request.telefono()));
        usuarioRepository.save(usuario);
        return usuarioMapper.toDetalle(usuario, rolesOf(id));
    }

    @Transactional
    public UsuarioDetalleResponse cambiarEstado(Long id, CambiarEstadoUsuarioRequest request, Long administradorActualId) {
        if (Boolean.FALSE.equals(request.activo()) && id.equals(administradorActualId)) {
            throw new OperacionAdministradorException("No puedes desactivar tu propia cuenta.");
        }
        Usuario usuario = findUsuario(id);
        if (Boolean.FALSE.equals(request.activo()) && isAdministrador(id) && usuarioRolRepository.countActiveAdministradores() <= 1) {
            throw new OperacionAdministradorException("No se puede desactivar al ultimo administrador activo.");
        }
        usuario.setActivo(request.activo());
        if (Boolean.TRUE.equals(request.activo())) {
            usuario.setBloqueadoHasta(null);
            usuario.setIntentosFallidos(0);
        }
        usuarioRepository.save(usuario);
        return usuarioMapper.toDetalle(usuario, rolesOf(id));
    }

    @Transactional
    public UsuarioRolesResponse actualizarRoles(
            Long id,
            ActualizarRolesUsuarioRequest request,
            Long administradorActualId) {
        Usuario usuario = findUsuario(id);
        List<NombreRol> nombresRoles = parseAndValidateRoleNames(request.roles());
        boolean usuarioEraAdministrador = isAdministrador(id);
        boolean usuarioSeraAdministrador = nombresRoles.contains(NombreRol.ADMINISTRADOR);
        if (usuarioEraAdministrador && !usuarioSeraAdministrador && usuarioRolRepository.countActiveAdministradores() <= 1) {
            throw new OperacionAdministradorException("No se puede retirar el ultimo rol administrador activo.");
        }
        List<Rol> roles = findActiveRoles(nombresRoles);
        usuarioRolRepository.deleteByUsuario_Id(id);
        usuarioRolRepository.saveAll(toUsuarioRoles(usuario, roles));
        return new UsuarioRolesResponse(usuario.getId(), usuario.getCorreo(), nombresRoles.stream().map(NombreRol::name).toList());
    }

    private Usuario findUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
    }

    private boolean isAdministrador(Long usuarioId) {
        return usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(usuarioId, NombreRol.ADMINISTRADOR);
    }

    private List<NombreRol> rolesOf(Long usuarioId) {
        return findRolesByUsuarioIds(List.of(usuarioId)).getOrDefault(usuarioId, List.of());
    }

    private Map<Long, List<NombreRol>> findRolesByUsuarioIds(List<Long> usuarioIds) {
        if (usuarioIds.isEmpty()) {
            return Map.of();
        }
        return usuarioRolRepository.findActivosByUsuarioIds(usuarioIds)
                .stream()
                .collect(Collectors.groupingBy(
                        usuarioRol -> usuarioRol.getUsuario().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(usuarioRol -> usuarioRol.getRol().getNombre(), Collectors.toList())));
    }

    private List<UsuarioRol> toUsuarioRoles(Usuario usuario, List<Rol> roles) {
        List<UsuarioRol> usuarioRoles = new ArrayList<>();
        for (Rol rol : roles) {
            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setUsuario(usuario);
            usuarioRol.setRol(rol);
            usuarioRoles.add(usuarioRol);
        }
        return usuarioRoles;
    }

    private List<Rol> findActiveRoles(List<NombreRol> nombresRoles) {
        Map<NombreRol, Rol> rolesByName = rolRepository.findAllByNombreIn(nombresRoles)
                .stream()
                .collect(Collectors.toMap(Rol::getNombre, Function.identity()));
        List<Rol> roles = new ArrayList<>();
        for (NombreRol nombreRol : nombresRoles) {
            Rol rol = rolesByName.get(nombreRol);
            if (rol == null) {
                throw new RolInvalidoException("El rol " + nombreRol.name() + " no existe.");
            }
            if (!Boolean.TRUE.equals(rol.getActivo())) {
                throw new RolInvalidoException("El rol " + nombreRol.name() + " no esta activo.");
            }
            roles.add(rol);
        }
        return roles;
    }

    private List<NombreRol> parseAndValidateRoleNames(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new RolInvalidoException("Debe asignarse al menos un rol.");
        }
        Map<NombreRol, NombreRol> uniqueRoles = new LinkedHashMap<>();
        for (String role : roles) {
            NombreRol nombreRol = parseRole(role);
            if (uniqueRoles.putIfAbsent(nombreRol, nombreRol) != null) {
                throw new RolInvalidoException("No se permiten roles duplicados.");
            }
        }
        return new ArrayList<>(uniqueRoles.keySet());
    }

    private NombreRol parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new RolInvalidoException("El rol no puede estar vacio.");
        }
        try {
            return NombreRol.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new RolInvalidoException("El rol " + role + " no existe.");
        }
    }

    private String normalizeEmail(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
