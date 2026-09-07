package com.veterinaria.backend.atencionmedica.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.backend.atencionmedica.dto.CrearAtencionMedicaRequest;
import com.veterinaria.backend.atencionmedica.dto.AtencionMedicaResponse;
import com.veterinaria.backend.atencionmedica.entity.AtencionMedica;
import com.veterinaria.backend.atencionmedica.repository.AtencionMedicaRepository;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;

@ExtendWith(MockitoExtension.class)
class AtencionMedicaServiceTest {

    @Mock private AtencionMedicaRepository atencionRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;

    @Test
    void creaAtencionYMarcaCitaAtendidaParaVeterinarioAsignado() {
        Usuario veterinario = usuario(7L, true);
        Cita cita = cita(10L, TipoCita.MEDICA, EstadoCita.CONFIRMADA, veterinario);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));
        when(usuarioRolRepository.existsByUsuario_IdAndRol_Nombre(7L,
                com.veterinaria.backend.usuario.enums.NombreRol.VETERINARIO)).thenReturn(true);
        when(atencionRepository.findByCitaId(10L)).thenReturn(Optional.empty());
        when(atencionRepository.saveAndFlush(any(AtencionMedica.class))).thenAnswer(invocation -> {
            AtencionMedica saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        AtencionMedicaResponse result = new AtencionMedicaService(
                atencionRepository, citaRepository, usuarioRolRepository)
                .crear(10L, 7L, request());

        assertThat(result.id()).isEqualTo(99L);
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.ATENDIDA);
        verify(atencionRepository).saveAndFlush(any(AtencionMedica.class));
    }

    private CrearAtencionMedicaRequest request() {
        return new CrearAtencionMedicaRequest(
                new BigDecimal("4.20"), new BigDecimal("38.2"), " Tos ", " Diagnóstico ", null,
                " Tratamiento ", null, " Receta ", null, " Observación ");
    }

    private Usuario usuario(Long id, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setActivo(activo);
        usuario.setPrimerNombre("Ana");
        usuario.setPrimerApellido("Veterinaria");
        return usuario;
    }

    private Cita cita(Long id, TipoCita tipo, EstadoCita estado, Usuario veterinario) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setTipoCita(tipo);
        cita.setEstado(estado);
        cita.setTrabajadorAsignado(veterinario);
        cita.setMascota(new Mascota());
        return cita;
    }
}
