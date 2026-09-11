package com.veterinaria.backend.vacuna.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;
import com.veterinaria.backend.atencionmedica.repository.AtencionMedicaRepository;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;
import com.veterinaria.backend.vacuna.dto.AplicarVacunaRequest;
import com.veterinaria.backend.vacuna.dto.VacunaAplicadaResponse;
import com.veterinaria.backend.vacuna.entity.Vacuna;
import com.veterinaria.backend.vacuna.entity.VacunaAplicada;
import com.veterinaria.backend.vacuna.exception.VacunaInactivaException;
import com.veterinaria.backend.vacuna.repository.VacunaAplicadaRepository;
import com.veterinaria.backend.vacuna.repository.VacunaRepository;

@ExtendWith(MockitoExtension.class)
class VacunaAplicadaServiceTest {

    @Mock private AtencionMedicaRepository atencionRepository;
    @Mock private VacunaRepository vacunaRepository;
    @Mock private VacunaAplicadaRepository aplicadaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;

    @Test
    void derivaFechaDesdeAtencionYNormalizaOpcionales() {
        Usuario veterinario = usuario(7L, true);
        AtencionMedica atencion = atencion(11L, veterinario, LocalDateTime.of(2026, 9, 10, 15, 30));
        Vacuna vacuna = vacuna(3L, true);
        when(atencionRepository.findByIdForVacunacion(11L)).thenReturn(Optional.of(atencion));
        when(vacunaRepository.findById(3L)).thenReturn(Optional.of(vacuna));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(veterinario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(7L, NombreRol.VETERINARIO)).thenReturn(true);
        when(aplicadaRepository.save(any(VacunaAplicada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VacunaAplicadaResponse result = service().aplicar(11L, 7L,
                new AplicarVacunaRequest(3L, LocalDate.of(2026, 9, 10), "  L-1  ", "  Nota  "));

        assertThat(result.fechaAplicacion()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(result.lote()).isEqualTo("L-1");
        assertThat(result.observaciones()).isEqualTo("Nota");
        verify(aplicadaRepository).save(any(VacunaAplicada.class));
    }

    @Test
    void rechazaVacunaInactiva() {
        Usuario veterinario = usuario(7L, true);
        when(atencionRepository.findByIdForVacunacion(11L)).thenReturn(Optional.of(
                atencion(11L, veterinario, LocalDateTime.now())));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(veterinario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(7L, NombreRol.VETERINARIO)).thenReturn(true);
        when(vacunaRepository.findById(3L)).thenReturn(Optional.of(vacuna(3L, false)));

        assertThatThrownBy(() -> service().aplicar(11L, 7L,
                new AplicarVacunaRequest(3L, null, null, null)))
                .isInstanceOf(VacunaInactivaException.class);
    }

    @Test
    void veterinarioAjenoNoPuedeAplicar() {
        Usuario responsable = usuario(7L, true);
        when(atencionRepository.findByIdForVacunacion(11L)).thenReturn(Optional.of(
                atencion(11L, responsable, LocalDateTime.now())));

        assertThatThrownBy(() -> service().aplicar(11L, 8L,
                new AplicarVacunaRequest(3L, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void permiteRepetirVacunaEnLaMismaAtencion() {
        Usuario veterinario = usuario(7L, true);
        AtencionMedica atencion = atencion(11L, veterinario, LocalDateTime.now());
        when(atencionRepository.findByIdForVacunacion(11L)).thenReturn(Optional.of(atencion));
        when(vacunaRepository.findById(3L)).thenReturn(Optional.of(vacuna(3L, true)));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(veterinario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(7L, NombreRol.VETERINARIO)).thenReturn(true);
        when(aplicadaRepository.save(any(VacunaAplicada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().aplicar(11L, 7L, new AplicarVacunaRequest(3L, null, null, null));
        service().aplicar(11L, 7L, new AplicarVacunaRequest(3L, null, null, null));

        verify(aplicadaRepository, org.mockito.Mockito.times(2)).save(any(VacunaAplicada.class));
    }

    @Test
    void rechazaProximaFechaAnteriorAFechaDerivada() {
        Usuario veterinario = usuario(7L, true);
        when(atencionRepository.findByIdForVacunacion(11L)).thenReturn(Optional.of(
                atencion(11L, veterinario, LocalDateTime.of(2026, 9, 10, 15, 30))));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(veterinario));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(7L, NombreRol.VETERINARIO)).thenReturn(true);
        when(vacunaRepository.findById(3L)).thenReturn(Optional.of(vacuna(3L, true)));

        assertThatThrownBy(() -> service().aplicar(11L, 7L,
                new AplicarVacunaRequest(3L, LocalDate.of(2026, 9, 9), null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private VacunaAplicadaService service() {
        return new VacunaAplicadaService(atencionRepository, vacunaRepository, aplicadaRepository,
                usuarioRepository, usuarioRolRepository);
    }

    private AtencionMedica atencion(Long id, Usuario veterinario, LocalDateTime fecha) {
        Cita cita = new Cita();
        cita.setTipoCita(TipoCita.MEDICA);
        cita.setEstado(EstadoCita.ATENDIDA);
        cita.setMascota(new Mascota());
        AtencionMedica atencion = new AtencionMedica();
        atencion.setId(id);
        atencion.setCita(cita);
        atencion.setVeterinario(veterinario);
        atencion.setFechaAtencion(fecha);
        return atencion;
    }

    private Usuario usuario(Long id, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setActivo(activo);
        return usuario;
    }

    private Vacuna vacuna(Long id, boolean activo) {
        Vacuna vacuna = new Vacuna();
        vacuna.setId(id);
        vacuna.setActivo(activo);
        vacuna.setNombre("Rabia");
        return vacuna;
    }
}
