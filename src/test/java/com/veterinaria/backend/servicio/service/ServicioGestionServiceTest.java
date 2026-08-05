package com.veterinaria.backend.servicio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.veterinaria.backend.servicio.dto.CrearServicioRequest;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.exception.ServicioDuplicadoException;
import com.veterinaria.backend.servicio.mapper.ServicioMapper;
import com.veterinaria.backend.servicio.repository.PrecioServicioTamanoRepository;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioGestionServiceTest {

    @Mock private ServicioRepository servicioRepository;
    @Mock private PrecioServicioTamanoRepository precioRepository;
    @Mock private ServicioMapper mapper;
    @InjectMocks private ServicioGestionService service;

    @Test
    void shouldNormalizeNameAndCreateActiveService() {
        CrearServicioRequest request = new CrearServicioRequest(
                "  Consulta general  ", "  Evaluacion  ", TipoServicio.MEDICO,
                null, 30);
        when(servicioRepository.existsByNombreIgnoreCase("Consulta general")).thenReturn(false);
        Servicio saved = new Servicio();
        saved.setActivo(true);
        when(servicioRepository.save(any(Servicio.class))).thenReturn(saved);

        service.crear(request);

        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);
        verify(servicioRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Consulta general");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Evaluacion");
        assertThat(captor.getValue().getActivo()).isTrue();
    }

    @Test
    void shouldRejectDuplicatedName() {
        CrearServicioRequest request = new CrearServicioRequest(
                "Baño", null, TipoServicio.PELUQUERIA, null, 60);
        when(servicioRepository.existsByNombreIgnoreCase("Baño")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(ServicioDuplicadoException.class);
        verify(servicioRepository, never()).save(any());
    }
}
