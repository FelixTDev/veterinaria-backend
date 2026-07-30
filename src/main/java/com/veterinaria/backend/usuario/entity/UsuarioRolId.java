package com.veterinaria.backend.usuario.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class UsuarioRolId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "rol_id")
    private Long rolId;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UsuarioRolId that)) {
            return false;
        }
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(rolId, that.rolId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, rolId);
    }
}
