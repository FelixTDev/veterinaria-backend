package com.veterinaria.backend.reporte.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SaldoPendienteProjection {
    Long getCitaId();
    LocalDateTime getFechaHoraInicio();
    Long getMascotaId();
    String getMascota();
    Long getClienteId();
    String getCliente();
    BigDecimal getTotalServicios();
    BigDecimal getTotalPagado();
    BigDecimal getSaldoPendiente();
}
