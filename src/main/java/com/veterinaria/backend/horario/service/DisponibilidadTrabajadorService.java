package com.veterinaria.backend.horario.service;

import java.time.LocalDateTime;

import com.veterinaria.backend.horario.dto.DisponibilidadTrabajadorResponse;
import com.veterinaria.backend.horario.repository.HorarioTrabajadorRepository;
import com.veterinaria.backend.horario.repository.IndisponibilidadTrabajadorRepository;
import com.veterinaria.backend.horario.exception.RangoHorarioInvalidoException;
import com.veterinaria.backend.horario.exception.TrabajadorNoEncontradoException;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisponibilidadTrabajadorService {
    private final UsuarioRepository usuarioRepository;
    private final HorarioTrabajadorRepository horarioRepository;
    private final IndisponibilidadTrabajadorRepository indisponibilidadRepository;

    public DisponibilidadTrabajadorService(UsuarioRepository usuarioRepository, HorarioTrabajadorRepository horarioRepository,
            IndisponibilidadTrabajadorRepository indisponibilidadRepository) {
        this.usuarioRepository = usuarioRepository; this.horarioRepository = horarioRepository; this.indisponibilidadRepository = indisponibilidadRepository;
    }

    @Transactional(readOnly = true)
    public DisponibilidadTrabajadorResponse consultar(Long trabajadorId, LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) throw new RangoHorarioInvalidoException("El rango solicitado no es valido.");
        if (!usuarioRepository.existsByIdAndActivoTrue(trabajadorId)) throw new TrabajadorNoEncontradoException("Trabajador no encontrado o inactivo.");
        int dia = inicio.getDayOfWeek().getValue();
        if (!horarioRepository.existsHorarioQueCubre(trabajadorId, dia, inicio.toLocalTime(), fin.toLocalTime())) return new DisponibilidadTrabajadorResponse(trabajadorId, inicio, fin, false, "Fuera del horario laboral.");
        if (indisponibilidadRepository.existsSolapamiento(trabajadorId, inicio, fin, null)) return new DisponibilidadTrabajadorResponse(trabajadorId, inicio, fin, false, "Existe una indisponibilidad en el intervalo.");
        return new DisponibilidadTrabajadorResponse(trabajadorId, inicio, fin, true, "Dentro del horario y sin indisponibilidades.");
    }
}
