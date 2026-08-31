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
        LocalDateTime inicio = LocalDateTime.now().plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = inicio.plusHours(2);
        Usuario usuario = new Usuario();
        usuario.setId(8L);
        usuario.setActivo(true);
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_NombreIn(8L,
                java.util.List.of(NombreRol.VETERINARIO, NombreRol.PELUQUERO))).thenReturn(true);
        when(repository.existsSolapamiento(8L, inicio, fin, null)).thenReturn(true);

        CrearIndisponibilidadRequest request = new CrearIndisponibilidadRequest(inicio, fin, "Capacitacion");

        assertThatThrownBy(() -> service.crear(8L, request))
                .isInstanceOf(IndisponibilidadSolapadaException.class);
    }
}
