package com.veterinaria.backend.reporte.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.cita.entity.Cita;

public interface DashboardReporteRepository extends JpaRepository<Cita, Long> {

    @Query("select count(c) from Cita c where c.fechaHoraInicio >= :desde and c.fechaHoraInicio < :hasta")
    long countCitas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(a) from AtencionMedica a where a.fechaAtencion >= :desde and a.fechaAtencion < :hasta")
    long countAtencionesMedicas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(a) from AtencionPeluqueria a where a.fechaAtencion >= :desde and a.fechaAtencion < :hasta")
    long countAtencionesPeluqueria(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(c) from Cliente c where c.activo = true")
    long countClientesActivos();

    @Query("select count(m) from Mascota m where m.activo = true")
    long countMascotasActivas();

    @Query("select count(s) from Servicio s where s.activo = true")
    long countServiciosActivos();
}
