package com.veterinaria.backend.atencionmedica.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record CrearAtencionMedicaRequest(
        @DecimalMin(value = "0.01") BigDecimal pesoKg,
        @DecimalMin(value = "30.0") @DecimalMax(value = "45.0") BigDecimal temperaturaC,
        @Size(max = 10000) String sintomas,
        @Size(max = 10000) String diagnostico,
        @Size(max = 10000) String motivoSinDiagnostico,
        @Size(max = 10000) String tratamiento,
        @Size(max = 10000) String motivoSinTratamiento,
        @Size(max = 10000) String receta,
        @Size(max = 10000) String motivoSinReceta,
        @Size(max = 10000) String observaciones) {
}
