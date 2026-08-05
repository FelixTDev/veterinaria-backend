package com.veterinaria.backend.usuario.mapper;

import java.util.List;

import com.veterinaria.backend.usuario.dto.RolResponse;
import com.veterinaria.backend.usuario.dto.UsuarioDetalleResponse;
import com.veterinaria.backend.usuario.dto.UsuarioResumenResponse;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;

import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResumenResponse toResumen(Usuario usuario, List<NombreRol> roles) {
        return new UsuarioResumenResponse(
                usuario.getId(),
                nombreCompleto(usuario),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getActivo(),
                roleNames(roles));
    }

    public UsuarioDetalleResponse toDetalle(Usuario usuario, List<NombreRol> roles) {
        return new UsuarioDetalleResponse(
                usuario.getId(),
                trimToNull(usuario.getPrimerNombre()),
                trimToNull(usuario.getSegundoNombre()),
                trimToNull(usuario.getPrimerApellido()),
                trimToNull(usuario.getSegundoApellido()),
                nombreCompleto(usuario),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getActivo(),
                roleNames(roles),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt(),
                usuario.getUltimoAcceso());
    }

    public RolResponse toRolResponse(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre().name(), rol.getDescripcion());
    }

    public String nombreCompleto(Usuario usuario) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, usuario.getPrimerNombre());
        appendIfPresent(builder, usuario.getSegundoNombre());
        appendIfPresent(builder, usuario.getPrimerApellido());
        appendIfPresent(builder, usuario.getSegundoApellido());
        return builder.toString();
    }

    private List<String> roleNames(List<NombreRol> roles) {
        return roles.stream()
                .map(NombreRol::name)
                .toList();
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(trimmed);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
