package com.veterinaria.backend.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.veterinaria.backend.cliente.dto.CrearClienteRequest;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.exception.DocumentoClienteDuplicadoException;
import com.veterinaria.backend.cliente.mapper.ClienteMapper;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteGestionServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteGestionService service;

    @Test
    void shouldNormalizeClientAndCreateItActive() {
        CrearClienteRequest request = new CrearClienteRequest(
                "  Maria ", null, " Torres ", null, TipoDocumento.DNI,
                " 123 ", null, " 999 ", " MARIA@TEST.COM ");
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "123"))
                .thenReturn(false);
        Cliente saved = new Cliente();
        saved.setActivo(true);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(saved);

        service.crear(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getPrimerNombre()).isEqualTo("Maria");
        assertThat(captor.getValue().getNumeroDocumento()).isEqualTo("123");
        assertThat(captor.getValue().getCorreo()).isEqualTo("maria@test.com");
        assertThat(captor.getValue().getActivo()).isTrue();
    }

    @Test
    void shouldRejectDuplicatedDocument() {
        CrearClienteRequest request = new CrearClienteRequest(
                "Maria", null, "Torres", null, TipoDocumento.DNI,
                "123", null, null, null);
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "123"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(DocumentoClienteDuplicadoException.class);
        verify(clienteRepository, never()).save(any());
    }
}
