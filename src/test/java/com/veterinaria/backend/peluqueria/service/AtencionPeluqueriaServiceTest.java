package com.veterinaria.backend.peluqueria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.peluqueria.entity.AtencionPeluqueria;
import com.veterinaria.backend.peluqueria.entity.FotoPeluqueria;
import com.veterinaria.backend.peluqueria.enums.TipoFoto;
import com.veterinaria.backend.peluqueria.repository.AtencionPeluqueriaRepository;
import com.veterinaria.backend.peluqueria.repository.FotoPeluqueriaRepository;
import com.veterinaria.backend.peluqueria.storage.ImageStorageService;
import com.veterinaria.backend.peluqueria.storage.StoredImage;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.peluqueria.dto.FotoPeluqueriaResponse;

@ExtendWith(MockitoExtension.class)
class AtencionPeluqueriaServiceTest {

    @Mock
    private AtencionPeluqueriaRepository atencionRepository;

    @Mock
    private FotoPeluqueriaRepository fotoRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Test
    void createsAttentionForAssignedHairdresserWithoutClosingAppointment() {
        Usuario peluquero = usuario(7L, true);
        Cita cita = cita(11L, peluquero, EstadoCita.CONFIRMADA);
        when(citaRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(cita));
        when(atencionRepository.existsByCitaId(11L)).thenReturn(false);
        when(atencionRepository.saveAndFlush(any(AtencionPeluqueria.class)))
                .thenAnswer(invocation -> {
                    AtencionPeluqueria atencion = invocation.getArgument(0);
                    atencion.setId(22L);
                    return atencion;
                });

        AtencionPeluqueriaService service = service();
        var response = service.crear(11L, 7L, EnumSet.of(NombreRol.PELUQUERO), "  Observacion  ");

        assertThat(response.id()).isEqualTo(22L);
        assertThat(response.estadoCita()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        verify(atencionRepository).saveAndFlush(any(AtencionPeluqueria.class));
    }

    @Test
    void rejectsEvidenceForHairdresserDifferentFromAppointmentAssignment() {
        Usuario assigned = usuario(7L, true);
        Cita cita = cita(11L, assigned, EstadoCita.CONFIRMADA);
        AtencionPeluqueria atencion = new AtencionPeluqueria();
        atencion.setId(22L);
        atencion.setCita(cita);
        atencion.setPeluquero(assigned);
        when(atencionRepository.findByIdWithCitaAndPeluquero(22L)).thenReturn(Optional.of(atencion));

        assertThatThrownBy(() -> service().agregarEvidencia(
                22L,
                99L,
                EnumSet.of(NombreRol.PELUQUERO),
                new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", jpegBytes()),
                TipoFoto.ANTES))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void uploadsAndPersistsEvidenceWithStorageMetadata() throws Exception {
        Usuario peluquero = usuario(7L, true);
        Cita cita = cita(11L, peluquero, EstadoCita.CONFIRMADA);
        AtencionPeluqueria atencion = new AtencionPeluqueria();
        atencion.setId(22L);
        atencion.setCita(cita);
        atencion.setPeluquero(peluquero);
        when(atencionRepository.findByIdWithCitaAndPeluquero(22L)).thenReturn(Optional.of(atencion));
        when(imageStorageService.upload(any(), eq("veterinaria/peluqueria/22/antes"), any()))
                .thenReturn(new StoredImage("https://cdn.test/foto.jpg", "veterinaria/peluqueria/22/antes/uuid"));
        when(fotoRepository.saveAndFlush(any(FotoPeluqueria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FotoPeluqueriaResponse response = service().agregarEvidencia(
                22L,
                7L,
                EnumSet.of(NombreRol.PELUQUERO),
                new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", jpegBytes()),
                TipoFoto.ANTES);

        assertThat(response.urlArchivo()).isEqualTo("https://cdn.test/foto.jpg");
        ArgumentCaptor<FotoPeluqueria> captor = ArgumentCaptor.forClass(FotoPeluqueria.class);
        verify(fotoRepository, times(1)).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStorageKey()).isEqualTo("veterinaria/peluqueria/22/antes/uuid");
    }

    @Test
    void rejectsClosingAttentionWithoutEvidenceAndKeepsAppointmentConfirmed() {
        Usuario peluquero = usuario(7L, true);
        Cita cita = cita(11L, peluquero, EstadoCita.CONFIRMADA);
        AtencionPeluqueria atencion = atencion(22L, cita, peluquero);
        when(atencionRepository.findByIdWithCitaAndPeluquero(22L)).thenReturn(Optional.of(atencion));
        when(citaRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(cita));
        when(fotoRepository.existsByAtencionPeluqueriaId(22L)).thenReturn(false);

        assertThatThrownBy(() -> service().cerrar(22L, 7L, EnumSet.of(NombreRol.PELUQUERO)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("evidencia");
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
    }

    @Test
    void closesAttentionWithOneEvidenceAndChangesAppointmentState() {
        Usuario peluquero = usuario(7L, true);
        Cita cita = cita(11L, peluquero, EstadoCita.CONFIRMADA);
        AtencionPeluqueria atencion = atencion(22L, cita, peluquero);
        when(atencionRepository.findByIdWithCitaAndPeluquero(22L)).thenReturn(Optional.of(atencion));
        when(citaRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(cita));
        when(fotoRepository.existsByAtencionPeluqueriaId(22L)).thenReturn(true);

        var response = service().cerrar(22L, 7L, EnumSet.of(NombreRol.PELUQUERO));

        assertThat(response.estadoCita()).isEqualTo(EstadoCita.ATENDIDA);
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void rejectsInvalidImageSignatureBeforeCallingStorage() {
        Usuario peluquero = usuario(7L, true);
        Cita cita = cita(11L, peluquero, EstadoCita.CONFIRMADA);
        when(atencionRepository.findByIdWithCitaAndPeluquero(22L)).thenReturn(Optional.of(atencion(22L, cita, peluquero)));

        assertThatThrownBy(() -> service().agregarEvidencia(
                22L, 7L, EnumSet.of(NombreRol.PELUQUERO),
                new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1, 2, 3}), TipoFoto.FINAL))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("contenido");
        verifyNoInteractions(imageStorageService);
    }

    @Test
    void compensatesStorageWhenDatabasePersistenceFails() {
        Usuario peluquero = usuario(7L, true);
        Cita cita = cita(11L, peluquero, EstadoCita.CONFIRMADA);
        when(atencionRepository.findByIdWithCitaAndPeluquero(22L)).thenReturn(Optional.of(atencion(22L, cita, peluquero)));
        when(imageStorageService.upload(any(), any(), any()))
                .thenReturn(new StoredImage("https://cdn.test/foto.jpg", "storage-key"));
        when(fotoRepository.saveAndFlush(any(FotoPeluqueria.class)))
                .thenThrow(new IllegalStateException("db failure"));

        assertThatThrownBy(() -> service().agregarEvidencia(
                22L, 7L, EnumSet.of(NombreRol.PELUQUERO),
                new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", jpegBytes()), TipoFoto.ANTES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db failure");
        verify(imageStorageService).delete("storage-key");
    }

    @ParameterizedTest
    @MethodSource("approvedImages")
    void acceptsApprovedImageSignatures(String contentType, byte[] content, String extension) {
        Usuario peluquero = usuario(7L, true);
        Cita cita = cita(11L, peluquero, EstadoCita.CONFIRMADA);
        when(atencionRepository.findByIdWithCitaAndPeluquero(22L)).thenReturn(Optional.of(atencion(22L, cita, peluquero)));
        when(imageStorageService.upload(any(), any(), any()))
                .thenReturn(new StoredImage("https://cdn.test/foto", "storage-key"));
        when(fotoRepository.saveAndFlush(any(FotoPeluqueria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service().agregarEvidencia(
                22L, 7L, EnumSet.of(NombreRol.PELUQUERO),
                new MockMultipartFile("archivo", "foto." + extension, contentType, content), TipoFoto.FINAL);

        assertThat(response.urlArchivo()).isEqualTo("https://cdn.test/foto");
    }

    static Stream<Arguments> approvedImages() {
        return Stream.of(
                Arguments.of("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "jpg"),
                Arguments.of("image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, "png"),
                Arguments.of("image/webp", new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}, "webp"));
    }

    private AtencionPeluqueriaService service() {
        return new AtencionPeluqueriaService(
                atencionRepository,
                fotoRepository,
                citaRepository,
                imageStorageService);
    }

    private Usuario usuario(Long id, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setActivo(activo);
        return usuario;
    }

    private Cita cita(Long id, Usuario peluquero, EstadoCita estado) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setTipoCita(TipoCita.PELUQUERIA);
        cita.setEstado(estado);
        cita.setTrabajadorAsignado(peluquero);
        return cita;
    }

    private AtencionPeluqueria atencion(Long id, Cita cita, Usuario peluquero) {
        AtencionPeluqueria atencion = new AtencionPeluqueria();
        atencion.setId(id);
        atencion.setCita(cita);
        atencion.setPeluquero(peluquero);
        return atencion;
    }

    private byte[] jpegBytes() {
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    }
}
