package com.veterinaria.backend.horario.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.veterinaria.backend.horario.dto.CrearIndisponibilidadRequest;
import com.veterinaria.backend.horario.exception.IndisponibilidadSolapadaException;
import com.veterinaria.backend.horario.mapper.IndisponibilidadTrabajadorMapper;
import com.veterinaria.backend.horario.repository.IndisponibilidadTrabajadorRepository;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndisponibilidadTrabajadorServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;
    @Mock private IndisponibilidadTrabajadorRepository repository;
    @Mock private IndisponibilidadTrabajadorMapper mapper;
    @InjectMocks private IndisponibilidadTrabajadorService service;

    @Test
    void shouldRejectOverlappingFutureUnavailability() {
        Usuario usuario = new Usuario();
        usuario.setId(8L);
        usuario.setActivo(true);
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_NombreIn(8L,
                java.util.List.of(NombreRol.VETERINARIO, NombreRol.PELUQUERO))).thenReturn(true);
        when(repository.existsSolapamiento(8L, LocalDateTime.of(2026, 8, 10, 10, 0),
                LocalDateTime.of(2026, 8, 10, 12, 0), null)).thenReturn(true);

        CrearIndisponibilidadRequest request = new CrearIndisponibilidadRequest(
                LocalDateTime.of(2026, 8, 10, 10, 0), LocalDateTime.of(2026, 8, 10, 12, 0), "Capacitacion");

        assertThatThrownBy(() -> service.crear(8L, request))
                .isInstanceOf(IndisponibilidadSolapadaException.class);
    }
}
