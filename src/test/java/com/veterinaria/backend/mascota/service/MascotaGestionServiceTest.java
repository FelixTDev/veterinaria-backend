package com.veterinaria.backend.mascota.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.dto.CrearMascotaRequest;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.exception.ClienteMascotaInactivoException;
import com.veterinaria.backend.mascota.mapper.MascotaMapper;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MascotaGestionServiceTest {

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MascotaMapper mascotaMapper;

    @InjectMocks
    private MascotaGestionService service;

    @Test
    void shouldRejectCreationForInactiveOwner() {
        Cliente cliente = new Cliente();
        cliente.setId(4L);
        cliente.setActivo(false);
        when(clienteRepository.findById(4L)).thenReturn(Optional.of(cliente));

        CrearMascotaRequest request = new CrearMascotaRequest(
                4L, "Luna", EspecieMascota.PERRO, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(ClienteMascotaInactivoException.class);
    }
}
