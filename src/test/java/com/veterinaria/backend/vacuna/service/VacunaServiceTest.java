package com.veterinaria.backend.vacuna.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.backend.vacuna.dto.ActualizarVacunaRequest;
import com.veterinaria.backend.vacuna.dto.CrearVacunaRequest;
import com.veterinaria.backend.vacuna.dto.VacunaResponse;
import com.veterinaria.backend.vacuna.entity.Vacuna;
import com.veterinaria.backend.vacuna.exception.VacunaDuplicadaException;
import com.veterinaria.backend.vacuna.repository.VacunaRepository;

@ExtendWith(MockitoExtension.class)
class VacunaServiceTest {

    @Mock private VacunaRepository vacunaRepository;

    @Test
    void creaVacunaNormalizandoTextoYActivandola() {
        when(vacunaRepository.existsByNombreIgnoreCase("Rabia")).thenReturn(false);
        when(vacunaRepository.saveAndFlush(any(Vacuna.class))).thenAnswer(invocation -> {
            Vacuna vacuna = invocation.getArgument(0);
            vacuna.setId(4L);
            return vacuna;
        });

        VacunaResponse result = new VacunaService(vacunaRepository).crear(
                new CrearVacunaRequest("  Rabia ", "  Esencial  "));

        assertThat(result.id()).isEqualTo(4L);
        assertThat(result.nombre()).isEqualTo("Rabia");
        assertThat(result.descripcion()).isEqualTo("Esencial");
        assertThat(result.activo()).isTrue();
    }

    @Test
    void rechazaNombreDuplicadoIgnorandoMayusculas() {
        when(vacunaRepository.existsByNombreIgnoreCase("rabia")).thenReturn(true);

        assertThatThrownBy(() -> new VacunaService(vacunaRepository).crear(
                new CrearVacunaRequest(" rabia ", null)))
                .isInstanceOf(VacunaDuplicadaException.class);
    }

    @Test
    void enEdicionExcluyeElMismoIdDeLaValidacionDeNombre() {
        Vacuna existente = new Vacuna();
        existente.setId(4L);
        existente.setNombre("Rabia");
        existente.setActivo(true);
        when(vacunaRepository.findById(4L)).thenReturn(Optional.of(existente));
        when(vacunaRepository.existsByNombreIgnoreCaseAndIdNot("Rabia nueva", 4L)).thenReturn(false);
        when(vacunaRepository.saveAndFlush(existente)).thenReturn(existente);

        VacunaResponse result = new VacunaService(vacunaRepository).actualizar(4L,
                new ActualizarVacunaRequest(" Rabia nueva ", "  Nueva descripción "));

        assertThat(result.nombre()).isEqualTo("Rabia nueva");
        assertThat(result.descripcion()).isEqualTo("Nueva descripción");
    }
}
