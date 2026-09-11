package com.veterinaria.backend.reporte.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.reporte.projection.SaldoPendienteProjection;

public interface SaldoReporteRepository extends JpaRepository<Cita, Long> {

    @Query(value = "with servicios as (select cita_id, sum(precio_aplicado) as total_servicios from cita_servicios group by cita_id), pagos_pagados as (select cita_id, sum(monto_total) as total_pagado from pagos where estado = 'PAGADO' group by cita_id) select c.id as cita_id, c.fecha_hora_inicio as fecha_hora_inicio, m.id as mascota_id, m.nombre as mascota, cl.id as cliente_id, concat_ws(' ', cl.primer_nombre, cl.segundo_nombre, cl.primer_apellido, cl.segundo_apellido) as cliente, s.total_servicios as total_servicios, coalesce(p.total_pagado, 0) as total_pagado, s.total_servicios - coalesce(p.total_pagado, 0) as saldo_pendiente from citas c join servicios s on s.cita_id = c.id join mascotas m on m.id = c.mascota_id join clientes cl on cl.id = m.cliente_id left join pagos_pagados p on p.cita_id = c.id where c.estado = 'ATENDIDA' and c.fecha_hora_inicio >= :desde and c.fecha_hora_inicio < :hasta and s.total_servicios - coalesce(p.total_pagado, 0) > 0 order by saldo_pendiente desc, c.fecha_hora_inicio asc, c.id asc",
            countQuery = "with servicios as (select cita_id, sum(precio_aplicado) as total_servicios from cita_servicios group by cita_id), pagos_pagados as (select cita_id, sum(monto_total) as total_pagado from pagos where estado = 'PAGADO' group by cita_id) select count(*) from citas c join servicios s on s.cita_id = c.id left join pagos_pagados p on p.cita_id = c.id where c.estado = 'ATENDIDA' and c.fecha_hora_inicio >= :desde and c.fecha_hora_inicio < :hasta and s.total_servicios - coalesce(p.total_pagado, 0) > 0",
            nativeQuery = true)
    Page<SaldoPendienteProjection> pendientes(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta, Pageable pageable);

    @Query(value = "with servicios as (select cita_id, sum(precio_aplicado) as total_servicios from cita_servicios group by cita_id), pagos_pagados as (select cita_id, sum(monto_total) as total_pagado from pagos where estado = 'PAGADO' group by cita_id) select coalesce(sum(s.total_servicios - coalesce(p.total_pagado, 0)), 0) from citas c join servicios s on s.cita_id = c.id left join pagos_pagados p on p.cita_id = c.id where c.estado = 'ATENDIDA' and c.fecha_hora_inicio >= :desde and c.fecha_hora_inicio < :hasta and s.total_servicios - coalesce(p.total_pagado, 0) > 0",
            nativeQuery = true)
    java.math.BigDecimal totalPendiente(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
