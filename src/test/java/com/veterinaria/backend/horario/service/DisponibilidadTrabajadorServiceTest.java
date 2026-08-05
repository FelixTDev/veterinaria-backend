package com.veterinaria.backend.horario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import com.veterinaria.backend.horario.dto.DisponibilidadTrabajadorResponse;
import com.veterinaria.backend.horario.repository.HorarioTrabajadorRepository;
import com.veterinaria.backend.horario.repository.IndisponibilidadTrabajadorRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisponibilidadTrabajadorServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HorarioTrabajadorRepository horarioRepository;
    @Mock private IndisponibilidadTrabajadorRepository indisponibilidadRepository;
    @InjectMocks private DisponibilidadTrabajadorService service;

    @Test
    void shouldReportAvailableWhenScheduleCoversIntervalWithoutBlock() {
        LocalDateTime inicio = LocalDateTime.of(2026, 8, 10, 9, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 8, 10, 10, 0);
        when(usuarioRepository.existsByIdAndActivoTrue(8L)).thenReturn(true);
        when(horarioRepository.existsHorarioQueCubre(8L, 1, inicio.toLocalTime(), fin.toLocalTime()))
                .thenReturn(true);
        when(indisponibilidadRepository.existsSolapamiento(8L, inicio, fin, null)).thenReturn(false);

        DisponibilidadTrabajadorResponse response = service.consultar(8L, inicio, fin);

        assertThat(response.disponible()).isTrue();
    }
}
