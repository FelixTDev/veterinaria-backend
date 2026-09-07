package com.veterinaria.backend.cita.repository;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class CitaSpecifications {

    private CitaSpecifications() {
    }

    public static Specification<Cita> fechaHoraInicioDesde(LocalDateTime fechaHoraInicioDesde) {
        return (root, query, criteriaBuilder) -> fechaHoraInicioDesde == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.greaterThanOrEqualTo(root.get("fechaHoraInicio"), fechaHoraInicioDesde);
    }

    public static Specification<Cita> fechaHoraInicioHasta(LocalDateTime fechaHoraInicioHasta) {
        return (root, query, criteriaBuilder) -> fechaHoraInicioHasta == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.lessThanOrEqualTo(root.get("fechaHoraInicio"), fechaHoraInicioHasta);
    }

    public static Specification<Cita> estadoEquals(EstadoCita estado) {
        return (root, query, criteriaBuilder) -> estado == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("estado"), estado);
    }

    public static Specification<Cita> tipoCitaEquals(TipoCita tipoCita) {
        return (root, query, criteriaBuilder) -> tipoCita == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("tipoCita"), tipoCita);
    }

    public static Specification<Cita> clienteIdEquals(Long clienteId) {
        return (root, query, criteriaBuilder) -> clienteId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.join("mascota", JoinType.INNER).join("cliente", JoinType.INNER).get("id"), clienteId);
    }

    public static Specification<Cita> mascotaIdEquals(Long mascotaId) {
        return (root, query, criteriaBuilder) -> mascotaId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.join("mascota", JoinType.INNER).get("id"), mascotaId);
    }

    public static Specification<Cita> trabajadorIdEquals(Long trabajadorId) {
        return (root, query, criteriaBuilder) -> trabajadorId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.join("trabajadorAsignado", JoinType.INNER).get("id"), trabajadorId);
    }

    public static Specification<Cita> search(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String normalized = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            Join<Object, Object> mascota = root.join("mascota", JoinType.INNER);
            Join<Object, Object> cliente = mascota.join("cliente", JoinType.INNER);
            Join<Object, Object> trabajador = root.join("trabajadorAsignado", JoinType.INNER);

            return criteriaBuilder.or(
                    ilike(criteriaBuilder, mascota.get("nombre"), normalized),
                    ilike(criteriaBuilder, criteriaBuilder.coalesce(mascota.get("raza"), ""), normalized),
                    ilike(criteriaBuilder, cliente.get("primerNombre"), normalized),
                    ilike(criteriaBuilder, criteriaBuilder.coalesce(cliente.get("segundoNombre"), ""), normalized),
                    ilike(criteriaBuilder, cliente.get("primerApellido"), normalized),
                    ilike(criteriaBuilder, criteriaBuilder.coalesce(cliente.get("segundoApellido"), ""), normalized),
                    ilike(criteriaBuilder, cliente.get("numeroDocumento"), normalized),
                    ilike(criteriaBuilder, criteriaBuilder.coalesce(cliente.get("correo"), ""), normalized),
                    ilike(criteriaBuilder, trabajador.get("primerNombre"), normalized),
                    ilike(criteriaBuilder, criteriaBuilder.coalesce(trabajador.get("segundoNombre"), ""), normalized),
                    ilike(criteriaBuilder, trabajador.get("primerApellido"), normalized),
                    ilike(criteriaBuilder, criteriaBuilder.coalesce(trabajador.get("segundoApellido"), ""), normalized),
                    ilike(criteriaBuilder, trabajador.get("correo"), normalized),
                    ilike(criteriaBuilder, criteriaBuilder.coalesce(root.get("motivoConsulta"), ""), normalized),
                    ilike(criteriaBuilder, criteriaBuilder.coalesce(root.get("observaciones"), ""), normalized));
        };
    }

    private static Predicate ilike(
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            jakarta.persistence.criteria.Expression<String> expression,
            String normalized) {
        return criteriaBuilder.like(criteriaBuilder.lower(expression), normalized);
    }
}
