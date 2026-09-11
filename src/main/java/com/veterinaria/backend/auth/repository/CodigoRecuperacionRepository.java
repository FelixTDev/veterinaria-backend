package com.veterinaria.backend.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.veterinaria.backend.auth.entity.CodigoRecuperacion;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    List<CodigoRecuperacion> findByUsuario_IdAndUsadoFalse(Long usuarioId);

    Optional<CodigoRecuperacion> findTopByUsuario_IdAndUsadoFalseOrderByCreatedAtDesc(Long usuarioId);

    Optional<CodigoRecuperacion> findByIdAndUsuario_Id(Long id, Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select codigo from CodigoRecuperacion codigo where codigo.id = :id and codigo.usuario.id = :usuarioId")
    Optional<CodigoRecuperacion> findByIdAndUsuarioIdForUpdate(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

}
