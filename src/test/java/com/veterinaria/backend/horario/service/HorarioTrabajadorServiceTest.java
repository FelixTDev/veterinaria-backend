package com.veterinaria.backend.horario.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Optional;

import com.veterinaria.backend.horario.dto.CrearHorarioRequest;
import com.veterinaria.backend.horario.entity.HorarioTrabajador;
import com.veterinaria.backend.horario.exception.TrabajadorNoProgramableException;
import com.veterinaria.backend.horario.mapper.HorarioTrabajadorMapper;
import com.veterinaria.backend.horario.repository.HorarioTrabajadorRepository;
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
class HorarioTrabajadorServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;
    @Mock private HorarioTrabajadorRepository horarioRepository;
    @Mock private HorarioTrabajadorMapper mapper;
    @InjectMocks private HorarioTrabajadorService service;

    @Test
    void shouldRejectReceptionistAsProgramableWorker() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setActivo(true);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_NombreIn(7L,
                java.util.List.of(NombreRol.VETERINARIO, NombreRol.PELUQUERO))).thenReturn(false);

        CrearHorarioRequest request = new CrearHorarioRequest(
                1, LocalTime.of(8, 0), LocalTime.of(12, 0), null, null);

        assertThatThrownBy(() -> service.crear(7L, request))
                .isInstanceOf(TrabajadorNoProgramableException.class);
    }

    @Test
    void shouldRejectInactiveProgramableWorker() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setActivo(false);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        CrearHorarioRequest request = new CrearHorarioRequest(
                1, LocalTime.of(8, 0), LocalTime.of(12, 0), null, null);

        assertThatThrownBy(() -> service.crear(7L, request))
                .isInstanceOf(TrabajadorNoProgramableException.class);
    }

    @Test
    void shouldRejectOverlappingSchedule() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setActivo(true);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_NombreIn(7L,
                java.util.List.of(NombreRol.VETERINARIO, NombreRol.PELUQUERO))).thenReturn(true);
        when(horarioRepository.existsSolapamiento(7L, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), null))
                .thenReturn(true);

        CrearHorarioRequest request = new CrearHorarioRequest(
                1, LocalTime.of(8, 0), LocalTime.of(12, 0), null, null);

        assertThatThrownBy(() -> service.crear(7L, request))
                .isInstanceOf(com.veterinaria.backend.horario.exception.HorarioSolapadoException.class);
    }
}
