package com.veterinaria.backend.cita.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.backend.cita.dto.CitaDetalleResponse;
import com.veterinaria.backend.cita.dto.CitaListadoFiltroRequest;
import com.veterinaria.backend.cita.dto.CitaResumenResponse;
import com.veterinaria.backend.cita.dto.CancelarCitaRequest;
import com.veterinaria.backend.cita.dto.CrearCitaRequest;
import com.veterinaria.backend.cita.dto.DisponibilidadCitaResponse;
import com.veterinaria.backend.cita.dto.MarcarNoAtendidaRequest;
import com.veterinaria.backend.cita.dto.ReprogramarCitaRequest;
import com.veterinaria.backend.cita.dto.ServicioCitaRequest;
import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.exception.CitaBadRequestException;
import com.veterinaria.backend.cita.exception.CitaConflictException;
import com.veterinaria.backend.cita.exception.CitaNoEncontradaException;
import com.veterinaria.backend.cita.mapper.CitaMapper;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.cita.repository.CitaServicioRepository;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.horario.dto.DisponibilidadTrabajadorResponse;
import com.veterinaria.backend.horario.service.DisponibilidadTrabajadorService;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TamanoMascota;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.exception.ServicioNoEncontradoException;
import com.veterinaria.backend.servicio.repository.PrecioServicioTamanoRepository;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.usuario.entity.Rol;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class CitaGestionServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private CitaServicioRepository citaServicioRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioRolRepository usuarioRolRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private PrecioServicioTamanoRepository precioServicioTamanoRepository;

    @Mock
    private DisponibilidadTrabajadorService disponibilidadTrabajadorService;

    private Clock fixedClock;
    private CitaGestionService service;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-08-31T15:00:30Z"), ZoneId.of("America/Lima"));
        service = new CitaGestionService(
                citaRepository,
                citaServicioRepository,
                mascotaRepository,
                usuarioRepository,
                usuarioRolRepository,
                servicioRepository,
                precioServicioTamanoRepository,
                disponibilidadTrabajadorService,
                new CitaMapper(),
                fixedClock);
    }

    @Test
    void shouldDeclareTransactionalCreateMethod() throws Exception {
        Method method = CitaGestionService.class.getMethod("crear", CrearCitaRequest.class, Long.class);

        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void shouldDeclareTask5ReadMethodsWithUserContextSignatures() {
        assertThatCode(() -> {
            Class<?> filtroClass = Class.forName("com.veterinaria.backend.cita.dto.CitaListadoFiltroRequest");
            Method listar = CitaGestionService.class.getMethod("listar", filtroClass, Long.class, Set.class);
            Method obtener = CitaGestionService.class.getMethod("obtener", Long.class, Long.class, Set.class);

            assertThat(listar.isAnnotationPresent(Transactional.class)).isTrue();
            assertThat(listar.getAnnotation(Transactional.class).readOnly()).isTrue();
            assertThat(obtener.isAnnotationPresent(Transactional.class)).isTrue();
            assertThat(obtener.getAnnotation(Transactional.class).readOnly()).isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldListPagedAppointmentsUsingSafeSortAndSingleBatchLoad() {
        Cita primera = cita(801L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 9, 0), futureAt(2, 9, 45));
        Cita segunda = cita(802L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 11, 0), futureAt(2, 11, 30));
        List<CitaServicio> servicios = List.of(
                citaServicio(segunda, 32L, TipoServicio.MEDICO, new BigDecimal("95.00"), 30),
                citaServicio(primera, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45));
        CitaListadoFiltroRequest filtro = new CitaListadoFiltroRequest(
                futureAt(1, 0, 0),
                futureAt(3, 23, 0),
                EstadoCita.CONFIRMADA,
                TipoCita.MEDICA,
                primera.getMascota().getCliente().getId(),
                primera.getMascota().getId(),
                20L,
                "  control  ",
                1,
                2,
                List.of("fechaHoraInicio,desc", "estado,asc"));

        when(citaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(segunda, primera), Pageable.ofSize(2).withPage(1), 5));
        when(citaServicioRepository.findByCitaIdIn(List.of(802L, 801L))).thenReturn(servicios);

        PaginaResponse<CitaResumenResponse> page = service.listar(filtro, 1L, Set.of(NombreRol.ADMINISTRADOR));

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.content()).extracting(CitaResumenResponse::id).containsExactly(802L, 801L);
        assertThat(page.content().get(0).servicios())
                .singleElement()
                .satisfies(servicio -> {
                    assertThat(servicio.id()).isEqualTo(32L);
                    assertThat(servicio.precioAplicado()).isEqualByComparingTo("95.00");
                });
        assertThat(page.content().get(1).servicios())
                .singleElement()
                .satisfies(servicio -> {
                    assertThat(servicio.id()).isEqualTo(30L);
                    assertThat(servicio.duracionAplicadaMinutos()).isEqualTo(45);
                });

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(citaRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getSort()).containsExactly(
                Sort.Order.desc("fechaHoraInicio"),
                Sort.Order.asc("estado"));
        verify(citaServicioRepository).findByCitaIdIn(List.of(802L, 801L));
    }

    @Test
    void shouldRejectInvalidPaginationSortAndDateRangeForListing() {
        assertThatThrownBy(() -> service.listar(
                filtroListado(0, 0, List.of("fechaHoraInicio,asc")),
                1L,
                Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("tamano");

        assertThatThrownBy(() -> service.listar(
                filtroListado(-1, 20, List.of("fechaHoraInicio,asc")),
                1L,
                Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("pagina");

        assertThatThrownBy(() -> service.listar(
                filtroListado(0, 20, List.of("mascota.nombre,asc")),
                1L,
                Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("sort");

        assertThatThrownBy(() -> service.listar(
                filtroListado(0, 20, List.of("fechaHoraInicio,sideways")),
                1L,
                Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("sort");

        assertThatThrownBy(() -> service.listar(
                new CitaListadoFiltroRequest(
                        futureAt(4, 12, 0),
                        futureAt(3, 12, 0),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        20,
                        List.of("fechaHoraInicio,asc")),
                1L,
                Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("rango");

        verifyNoInteractions(citaRepository, citaServicioRepository);
    }

    @Test
    void shouldRejectSortWithTrailingComma() {
        assertThatThrownBy(() -> service.listar(
                filtroListado(0, 20, List.of("fechaHoraInicio,")),
                1L,
                Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("sort");

        verifyNoInteractions(citaRepository, citaServicioRepository);
    }

    @Test
    void shouldRejectSortWithEmptyField() {
        assertThatThrownBy(() -> service.listar(
                filtroListado(0, 20, List.of(",asc")),
                1L,
                Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("sort");

        verifyNoInteractions(citaRepository, citaServicioRepository);
    }

    @Test
    void shouldRejectSortWithExtraSegments() {
        assertThatThrownBy(() -> service.listar(
                filtroListado(0, 20, List.of("fechaHoraInicio,,desc")),
                1L,
                Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("sort");

        verifyNoInteractions(citaRepository, citaServicioRepository);
    }

    @Test
    void shouldForceAuthenticatedWorkerForProfessionalListing() {
        when(citaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        service.listar(
                new CitaListadoFiltroRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        999L,
                        null,
                        0,
                        20,
                        List.of("fechaHoraInicio,asc")),
                20L,
                Set.of(NombreRol.VETERINARIO, NombreRol.PELUQUERO));

        ArgumentCaptor<Specification<Cita>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(citaRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertThatTrabajadorIdFiltrado(specCaptor.getValue(), 20L);
        verify(citaServicioRepository, never()).findByCitaIdIn(anyList());
    }

    @Test
    void shouldRejectProfessionalListingWithIncompatibleTypeFilter() {
        assertThatThrownBy(() -> service.listar(
                new CitaListadoFiltroRequest(
                        null,
                        null,
                        null,
                        TipoCita.PELUQUERIA,
                        null,
                        null,
                        null,
                        null,
                        0,
                        20,
                        List.of("fechaHoraInicio,asc")),
                20L,
                Set.of(NombreRol.VETERINARIO)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso");

        verifyNoInteractions(citaRepository, citaServicioRepository);
    }

    @Test
    void shouldReturnDetalleForAssignedCompatibleProfessional() {
        Cita cita = cita(810L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 14, 0), futureAt(2, 14, 45));
        List<CitaServicio> servicios = List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45));
        when(citaRepository.findById(810L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(810L))).thenReturn(servicios);

        CitaDetalleResponse response = service.obtener(810L, 20L, Set.of(NombreRol.VETERINARIO));

        assertThat(response.id()).isEqualTo(810L);
        assertThat(response.trabajador().id()).isEqualTo(20L);
        assertThat(response.servicios()).singleElement().satisfies(servicio -> assertThat(servicio.id()).isEqualTo(30L));
        verify(citaServicioRepository).findByCitaIdIn(List.of(810L));
    }

    @Test
    void shouldRejectDetalleForForeignOrIncompatibleProfessionalAndReturn404WhenMissing() {
        Cita citaAjena = cita(811L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 15, 0), futureAt(2, 15, 45));
        Cita citaIncompatible = cita(812L, EstadoCita.CONFIRMADA, TipoCita.PELUQUERIA, 22L, futureAt(2, 16, 0), futureAt(2, 16, 35));
        when(citaRepository.findById(811L)).thenReturn(Optional.of(citaAjena));
        when(citaRepository.findById(812L)).thenReturn(Optional.of(citaIncompatible));
        when(citaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(811L, 21L, Set.of(NombreRol.VETERINARIO)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso");
        assertThatThrownBy(() -> service.obtener(812L, 22L, Set.of(NombreRol.VETERINARIO)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso");
        assertThatThrownBy(() -> service.obtener(999L, 1L, Set.of(NombreRol.ADMINISTRADOR)))
                .isInstanceOf(CitaNoEncontradaException.class)
                .hasMessageContaining("no encontrada");

        verify(citaServicioRepository, never()).findByCitaIdIn(List.of(811L));
        verify(citaServicioRepository, never()).findByCitaIdIn(List.of(812L));
        verify(citaServicioRepository, never()).findByCitaIdIn(List.of(999L));
    }

    @Test
    void shouldCentralizeOccupyingStatesAsPendingAndConfirmedOnly() {
        assertThat(EstadoAgendaCita.ocupantes())
                .containsExactlyInAnyOrder(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
    }

    @Test
    void shouldCreateMedicalAppointmentUsingBackendSnapshotsAndPendingState() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                20L,
                inicio,
                List.of(new ServicioCitaRequest(30L, null)),
                "  Control anual  ",
                "  Llegar en ayunas  ");

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(20L, "Julio", "Paredes");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true);

        stubMascotaRegistradorServiciosYTrabajador(mascota, registrador, List.of(servicio), trabajador, roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, inicio.plusMinutes(45)))
                .thenReturn(disponible(20L, inicio, inicio.plusMinutes(45)));
        when(citaRepository.existsSolapamiento(20L, EstadoAgendaCita.ocupantes(), inicio, inicio.plusMinutes(45), null))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> {
            Cita cita = invocation.getArgument(0);
            cita.setId(900L);
            return cita;
        });
        when(citaServicioRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CitaDetalleResponse response = service.crear(request, 40L);

        assertThat(response.id()).isEqualTo(900L);
        assertThat(response.tipoCita()).isEqualTo(TipoCita.MEDICA);
        assertThat(response.estado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(response.fechaHoraInicio()).isEqualTo(inicio);
        assertThat(response.fechaHoraFin()).isEqualTo(inicio.plusMinutes(45));
        assertThat(response.motivoConsulta()).isEqualTo("Control anual");
        assertThat(response.observaciones()).isEqualTo("Llegar en ayunas");
        assertThat(response.cliente().id()).isEqualTo(100L);
        assertThat(response.mascota().id()).isEqualTo(10L);
        assertThat(response.trabajador().id()).isEqualTo(20L);
        assertThat(response.registradoPor().id()).isEqualTo(40L);
        assertThat(response.servicios())
                .singleElement()
                .satisfies(servicioResponse -> {
                    assertThat(servicioResponse.id()).isEqualTo(30L);
                    assertThat(servicioResponse.precioServicioTamanoId()).isNull();
                    assertThat(servicioResponse.precioAplicado()).isEqualByComparingTo("80.00");
                    assertThat(servicioResponse.duracionAplicadaMinutos()).isEqualTo(45);
                });

        ArgumentCaptor<Cita> citaCaptor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(citaCaptor.capture());
        assertThat(citaCaptor.getValue().getMascota()).isSameAs(mascota);
        assertThat(citaCaptor.getValue().getTrabajadorAsignado()).isSameAs(trabajador);
        assertThat(citaCaptor.getValue().getRegistradoPor()).isSameAs(registrador);
        assertThat(citaCaptor.getValue().getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(citaCaptor.getValue().getTipoCita()).isEqualTo(TipoCita.MEDICA);
        assertThat(citaCaptor.getValue().getFechaHoraFin()).isEqualTo(inicio.plusMinutes(45));

        List<CitaServicio> citaServicios = capturarCitaServiciosGuardados();
        assertThat(citaServicios).hasSize(1);
        assertThat(citaServicios.get(0).getServicio()).isSameAs(servicio);
        assertThat(citaServicios.get(0).getPrecioServicioTamano()).isNull();
        assertThat(citaServicios.get(0).getPrecioAplicado()).isEqualByComparingTo("80.00");
        assertThat(citaServicios.get(0).getDuracionAplicadaMinutos()).isEqualTo(45);

        InOrder inOrder = inOrder(usuarioRepository, disponibilidadTrabajadorService, citaRepository);
        inOrder.verify(usuarioRepository).findByIdForUpdate(20L);
        inOrder.verify(disponibilidadTrabajadorService).consultar(20L, inicio, inicio.plusMinutes(45));
        inOrder.verify(citaRepository).existsSolapamiento(20L, EstadoAgendaCita.ocupantes(), inicio, inicio.plusMinutes(45), null);

        verify(servicioRepository).findAllById(anyCollection());
        verifyNoInteractions(precioServicioTamanoRepository);
    }

    @Test
    void shouldCreateGroomingAppointmentUsingActiveTarifaSnapshot() {
        LocalDateTime inicio = futureAt(3, 15, 0);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                21L,
                inicio,
                List.of(new ServicioCitaRequest(31L, 41L)),
                "Baño y corte",
                null);

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(21L, "Lucia", "Cortez");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(31L, TipoServicio.PELUQUERIA, new BigDecimal("50.00"), 30, true);
        PrecioServicioTamano tarifa = tarifa(41L, servicio, new BigDecimal("65.50"), 35, true);

        stubMascotaRegistradorServiciosYTrabajador(mascota, registrador, List.of(servicio), trabajador, roles(NombreRol.PELUQUERO));
        when(precioServicioTamanoRepository.findByIdInWithServicio(Set.of(41L))).thenReturn(List.of(tarifa));
        when(disponibilidadTrabajadorService.consultar(21L, inicio, inicio.plusMinutes(35)))
                .thenReturn(disponible(21L, inicio, inicio.plusMinutes(35)));
        when(citaRepository.existsSolapamiento(21L, EstadoAgendaCita.ocupantes(), inicio, inicio.plusMinutes(35), null))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> {
            Cita cita = invocation.getArgument(0);
            cita.setId(901L);
            return cita;
        });
        when(citaServicioRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CitaDetalleResponse response = service.crear(request, 40L);

        assertThat(response.tipoCita()).isEqualTo(TipoCita.PELUQUERIA);
        assertThat(response.fechaHoraFin()).isEqualTo(inicio.plusMinutes(35));
        assertThat(response.servicios())
                .singleElement()
                .satisfies(servicioResponse -> {
                    assertThat(servicioResponse.id()).isEqualTo(31L);
                    assertThat(servicioResponse.precioServicioTamanoId()).isEqualTo(41L);
                    assertThat(servicioResponse.precioAplicado()).isEqualByComparingTo("65.50");
                    assertThat(servicioResponse.duracionAplicadaMinutos()).isEqualTo(35);
                });
    }

    @Test
    void shouldCreateAppointmentWithTwoServicesPreservingRequestOrderAndAccumulatedDuration() {
        LocalDateTime inicio = futureAt(4, 9, 30);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                20L,
                inicio,
                List.of(
                        new ServicioCitaRequest(31L, 41L),
                        new ServicioCitaRequest(30L, null)),
                "  Sesion combinada  ",
                "  orden exacto  ");

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(20L, "Julio", "Paredes");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio primero = servicio(31L, TipoServicio.MEDICO, new BigDecimal("60.00"), 30, true);
        Servicio segundo = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true);
        PrecioServicioTamano tarifaPrimera = tarifa(41L, primero, new BigDecimal("95.50"), 50, true);

        stubMascotaRegistradorServiciosYTrabajador(
                mascota,
                registrador,
                List.of(segundo, primero),
                trabajador,
                roles(NombreRol.VETERINARIO));
        when(precioServicioTamanoRepository.findByIdInWithServicio(Set.of(41L))).thenReturn(List.of(tarifaPrimera));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, inicio.plusMinutes(95)))
                .thenReturn(disponible(20L, inicio, inicio.plusMinutes(95)));
        when(citaRepository.existsSolapamiento(20L, EstadoAgendaCita.ocupantes(), inicio, inicio.plusMinutes(95), null))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> {
            Cita cita = invocation.getArgument(0);
            cita.setId(904L);
            return cita;
        });
        when(citaServicioRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CitaDetalleResponse response = service.crear(request, 40L);

        assertThat(response.id()).isEqualTo(904L);
        assertThat(response.fechaHoraInicio()).isEqualTo(inicio);
        assertThat(response.fechaHoraFin()).isEqualTo(inicio.plusMinutes(95));
        assertThat(response.motivoConsulta()).isEqualTo("Sesion combinada");
        assertThat(response.observaciones()).isEqualTo("orden exacto");
        assertThat(response.servicios())
                .extracting(
                        servicioResponse -> servicioResponse.id(),
                        servicioResponse -> servicioResponse.precioServicioTamanoId(),
                        servicioResponse -> servicioResponse.precioAplicado(),
                        servicioResponse -> servicioResponse.duracionAplicadaMinutos())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(31L, 41L, new BigDecimal("95.50"), 50),
                        org.assertj.core.groups.Tuple.tuple(30L, null, new BigDecimal("80.00"), 45));

        List<CitaServicio> citaServicios = capturarCitaServiciosGuardados();
        assertThat(citaServicios)
                .extracting(
                        citaServicio -> citaServicio.getServicio().getId(),
                        citaServicio -> citaServicio.getPrecioServicioTamano() != null
                                ? citaServicio.getPrecioServicioTamano().getId()
                                : null,
                        CitaServicio::getPrecioAplicado,
                        CitaServicio::getDuracionAplicadaMinutos)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(31L, 41L, new BigDecimal("95.50"), 50),
                        org.assertj.core.groups.Tuple.tuple(30L, null, new BigDecimal("80.00"), 45));

        verify(servicioRepository).findAllById(Set.of(31L, 30L));
        verify(precioServicioTamanoRepository).findByIdInWithServicio(Set.of(41L));
    }

    @Test
    void shouldAllowProfessionalWithBothRoles() {
        LocalDateTime inicio = futureAt(2, 11, 0);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                22L,
                inicio,
                List.of(new ServicioCitaRequest(32L, null)),
                "Control doble rol",
                null);

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(22L, "Andrea", "Mixta");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(32L, TipoServicio.MEDICO, BigDecimal.ZERO, 20, true);

        stubMascotaRegistradorServiciosYTrabajador(
                mascota,
                registrador,
                List.of(servicio),
                trabajador,
                roles(NombreRol.VETERINARIO, NombreRol.PELUQUERO));
        when(disponibilidadTrabajadorService.consultar(22L, inicio, inicio.plusMinutes(20)))
                .thenReturn(disponible(22L, inicio, inicio.plusMinutes(20)));
        when(citaRepository.existsSolapamiento(22L, EstadoAgendaCita.ocupantes(), inicio, inicio.plusMinutes(20), null))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> {
            Cita cita = invocation.getArgument(0);
            cita.setId(902L);
            return cita;
        });
        when(citaServicioRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.crear(request, 40L)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDuplicatedServices() {
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                20L,
                futureAt(2, 10, 0),
                List.of(
                        new ServicioCitaRequest(30L, null),
                        new ServicioCitaRequest(30L, 41L)),
                "Duplicado",
                null);

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("duplicados");

        verifyNoInteractions(servicioRepository, citaRepository, citaServicioRepository);
    }

    @Test
    void shouldRejectMixedServiceTypes() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                20L,
                inicio,
                List.of(
                        new ServicioCitaRequest(30L, null),
                        new ServicioCitaRequest(31L, null)),
                "Mixta",
                null);

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(anyCollection())).thenReturn(List.of(
                servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true),
                servicio(31L, TipoServicio.PELUQUERIA, new BigDecimal("55.00"), 30, true)));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("mezclar");
    }

    @Test
    void shouldRejectInactiveMascota() {
        CrearCitaRequest request = requestMedicaBasica(futureAt(2, 10, 0));
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        mascota.setActivo(Boolean.FALSE);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("Mascota");
    }

    @Test
    void shouldRejectInactiveCliente() {
        CrearCitaRequest request = requestMedicaBasica(futureAt(2, 10, 0));
        Cliente cliente = clienteActivo(100L);
        cliente.setActivo(Boolean.FALSE);
        Mascota mascota = mascotaActiva(10L, cliente);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("cliente");
    }

    @Test
    void shouldRejectInactiveService() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = requestMedicaBasica(inicio);
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, false);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(Set.of(30L))).thenReturn(List.of(servicio));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("Servicio");
    }

    @Test
    void shouldRejectNullPrecioBaseWithoutTarifa() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = requestMedicaBasica(inicio);
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, null, 45, true);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(Set.of(30L))).thenReturn(List.of(servicio));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("precio base");
    }

    @Test
    void shouldRejectTarifaThatDoesNotBelongToService() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                21L,
                inicio,
                List.of(new ServicioCitaRequest(31L, 41L)),
                "Baño",
                null);

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(31L, TipoServicio.PELUQUERIA, new BigDecimal("50.00"), 30, true);
        Servicio otroServicio = servicio(99L, TipoServicio.PELUQUERIA, new BigDecimal("70.00"), 40, true);
        PrecioServicioTamano tarifa = tarifa(41L, otroServicio, new BigDecimal("70.00"), 40, true);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(Set.of(31L))).thenReturn(List.of(servicio));
        when(precioServicioTamanoRepository.findByIdInWithServicio(Set.of(41L))).thenReturn(List.of(tarifa));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("tarifa");
    }

    @Test
    void shouldRejectNonexistentTarifaWithNotFoundException() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                21L,
                inicio,
                List.of(new ServicioCitaRequest(31L, 41L)),
                "Baño",
                null);

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(31L, TipoServicio.PELUQUERIA, new BigDecimal("50.00"), 30, true);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(Set.of(31L))).thenReturn(List.of(servicio));
        when(precioServicioTamanoRepository.findByIdInWithServicio(Set.of(41L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(ServicioNoEncontradoException.class)
                .hasMessageContaining("Tarifa");
    }

    @Test
    void shouldRejectInactiveTarifa() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                21L,
                inicio,
                List.of(new ServicioCitaRequest(31L, 41L)),
                "Baño",
                null);

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(31L, TipoServicio.PELUQUERIA, new BigDecimal("50.00"), 30, true);
        PrecioServicioTamano tarifaInactiva = tarifa(41L, servicio, new BigDecimal("65.50"), 35, false);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(Set.of(31L))).thenReturn(List.of(servicio));
        when(precioServicioTamanoRepository.findByIdInWithServicio(Set.of(41L))).thenReturn(List.of(tarifaInactiva));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("tarifa");
    }

    @Test
    void shouldRejectInactiveProfessional() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = requestMedicaBasica(inicio);
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(20L, "Julio", "Paredes");
        trabajador.setActivo(Boolean.FALSE);
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(Set.of(30L))).thenReturn(List.of(servicio));
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(trabajador));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("Trabajador");
    }

    @Test
    void shouldRejectIncompatibleProfessionalRole() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = requestMedicaBasica(inicio);
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(20L, "Julio", "Paredes");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true);

        stubMascotaRegistradorServiciosYTrabajador(
                mascota,
                registrador,
                List.of(servicio),
                trabajador,
                roles(NombreRol.PELUQUERO));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("compatible");
    }

    @Test
    void shouldRejectPastStartDate() {
        CrearCitaRequest request = requestMedicaBasica(now().minusMinutes(30));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("futuro");

        verifyNoInteractions(mascotaRepository, usuarioRepository, servicioRepository);
    }

    @Test
    void shouldRejectCreateWhenStartIsAFewSecondsBeforeNowWithinSameMinute() {
        CrearCitaRequest request = requestMedicaBasica(now().minusSeconds(5));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void shouldRejectAppointmentThatCrossesDayBoundary() {
        LocalDateTime inicio = futureAt(2, 23, 40);
        CrearCitaRequest request = new CrearCitaRequest(
                10L,
                20L,
                inicio,
                List.of(new ServicioCitaRequest(30L, null)),
                "Control noche",
                null);

        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 30, true);

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(Set.of(30L))).thenReturn(List.of(servicio));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("dia");
    }

    @Test
    void shouldRejectWhenBaseAvailabilityReportsRest() {
        LocalDateTime inicio = futureAt(2, 13, 0);
        CrearCitaRequest request = requestMedicaBasica(inicio);
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(20L, "Julio", "Paredes");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true);

        stubMascotaRegistradorServiciosYTrabajador(mascota, registrador, List.of(servicio), trabajador, roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, inicio.plusMinutes(45)))
                .thenReturn(new DisponibilidadTrabajadorResponse(20L, inicio, inicio.plusMinutes(45), false, "Fuera del horario laboral."));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("horario laboral");

        verify(citaRepository, never()).existsSolapamiento(any(), anySet(), any(), any(), any());
    }

    @Test
    void shouldRejectWhenBaseAvailabilityReportsIndisponibilidad() {
        LocalDateTime inicio = futureAt(2, 16, 0);
        CrearCitaRequest request = requestMedicaBasica(inicio);
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(20L, "Julio", "Paredes");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true);

        stubMascotaRegistradorServiciosYTrabajador(mascota, registrador, List.of(servicio), trabajador, roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, inicio.plusMinutes(45)))
                .thenReturn(new DisponibilidadTrabajadorResponse(
                        20L,
                        inicio,
                        inicio.plusMinutes(45),
                        false,
                        "Existe una indisponibilidad en el intervalo."));

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("indisponibilidad");

        verify(citaRepository, never()).existsSolapamiento(any(), anySet(), any(), any(), any());
    }

    @Test
    void shouldRejectOverlappingAppointmentsUsingCentralizedOccupyingStates() {
        LocalDateTime inicio = futureAt(2, 10, 0);
        CrearCitaRequest request = requestMedicaBasica(inicio);
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(20L, "Julio", "Paredes");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true);

        stubMascotaRegistradorServiciosYTrabajador(mascota, registrador, List.of(servicio), trabajador, roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, inicio.plusMinutes(45)))
                .thenReturn(disponible(20L, inicio, inicio.plusMinutes(45)));
        when(citaRepository.existsSolapamiento(20L, EstadoAgendaCita.ocupantes(), inicio, inicio.plusMinutes(45), null))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crear(request, 40L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("solapa");
    }

    @Test
    void shouldAllowContiguousAppointments() {
        LocalDateTime inicio = futureAt(2, 11, 0);
        CrearCitaRequest request = requestMedicaBasica(inicio);
        Mascota mascota = mascotaActiva(10L, clienteActivo(100L));
        Usuario trabajador = usuarioActivo(20L, "Julio", "Paredes");
        Usuario registrador = usuarioActivo(40L, "Paola", "Ruiz");
        Servicio servicio = servicio(30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45, true);

        stubMascotaRegistradorServiciosYTrabajador(mascota, registrador, List.of(servicio), trabajador, roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, inicio.plusMinutes(45)))
                .thenReturn(disponible(20L, inicio, inicio.plusMinutes(45)));
        when(citaRepository.existsSolapamiento(20L, EstadoAgendaCita.ocupantes(), inicio, inicio.plusMinutes(45), null))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> {
            Cita cita = invocation.getArgument(0);
            cita.setId(903L);
            return cita;
        });
        when(citaServicioRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CitaDetalleResponse response = service.crear(request, 40L);

        assertThat(response.id()).isEqualTo(903L);
        assertThat(response.estado()).isEqualTo(EstadoCita.PENDIENTE);
    }

    @Test
    void shouldConfirmPendingAppointment() {
        Cita cita = cita(700L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 20L, futureAt(2, 10, 0), futureAt(2, 10, 45));
        List<CitaServicio> servicios = List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45));
        when(citaRepository.findByIdForUpdate(700L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(700L))).thenReturn(servicios);

        CitaDetalleResponse response = service.confirmar(700L);

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(response.estado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(response.motivoCancelacion()).isNull();
        assertThat(response.motivoNoAtencion()).isNull();
        verify(usuarioRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldRejectConfirmForRepeatedOrInvalidStates() {
        Cita confirmada = cita(701L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 11, 0), futureAt(2, 11, 45));
        Cita cancelada = cita(702L, EstadoCita.CANCELADA, TipoCita.MEDICA, 20L, futureAt(2, 12, 0), futureAt(2, 12, 45));
        when(citaRepository.findByIdForUpdate(701L)).thenReturn(Optional.of(confirmada));
        when(citaRepository.findByIdForUpdate(702L)).thenReturn(Optional.of(cancelada));

        assertThatThrownBy(() -> service.confirmar(701L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("estado");
        assertThatThrownBy(() -> service.confirmar(702L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("estado");

        verify(citaServicioRepository, never()).findByCitaIdIn(anyCollection());
    }

    @Test
    void shouldCancelPendingAppointmentWithNormalizedReason() {
        Cita cita = cita(703L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 20L, futureAt(2, 13, 0), futureAt(2, 13, 45));
        List<CitaServicio> servicios = List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45));
        when(citaRepository.findByIdForUpdate(703L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(703L))).thenReturn(servicios);

        CitaDetalleResponse response = service.cancelar(703L, new CancelarCitaRequest("  Cliente reagendo  "));

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cita.getMotivoCancelacion()).isEqualTo("Cliente reagendo");
        assertThat(response.estado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(response.motivoCancelacion()).isEqualTo("Cliente reagendo");
    }

    @Test
    void shouldCancelConfirmedAppointment() {
        Cita cita = cita(704L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 14, 0), futureAt(2, 14, 45));
        when(citaRepository.findByIdForUpdate(704L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(704L)))
                .thenReturn(List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45)));

        CitaDetalleResponse response = service.cancelar(704L, new CancelarCitaRequest("Motivo valido"));

        assertThat(response.estado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cita.getMotivoCancelacion()).isEqualTo("Motivo valido");
    }

    @Test
    void shouldRejectCancelWithoutNormalizedReasonOrFromTerminalState() {
        Cita activa = cita(705L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 20L, futureAt(2, 10, 0), futureAt(2, 10, 45));
        Cita terminal = cita(718L, EstadoCita.ATENDIDA, TipoCita.MEDICA, 20L, pastAt(1, 10, 0), pastAt(1, 10, 45));
        when(citaRepository.findByIdForUpdate(705L)).thenReturn(Optional.of(activa));
        when(citaRepository.findByIdForUpdate(718L)).thenReturn(Optional.of(terminal));

        assertThatThrownBy(() -> service.cancelar(705L, new CancelarCitaRequest("   ")))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("motivo");
        assertThatThrownBy(() -> service.cancelar(718L, new CancelarCitaRequest("Ya cerrada")))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("estado");
    }

    @Test
    void shouldMarkPendingAppointmentAsNoAtendidaWhenStartAlreadyArrived() {
        LocalDateTime inicio = now().minusMinutes(5);
        Cita cita = cita(706L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 20L, inicio, inicio.plusMinutes(45));
        when(citaRepository.findByIdForUpdate(706L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(706L)))
                .thenReturn(List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45)));

        CitaDetalleResponse response = service.marcarNoAtendida(706L, new MarcarNoAtendidaRequest("  No se presentó  "));

        assertThat(response.estado()).isEqualTo(EstadoCita.NO_ATENDIDA);
        assertThat(cita.getMotivoNoAtencion()).isEqualTo("No se presentó");
    }

    @Test
    void shouldMarkConfirmedAppointmentAsNoAtendidaWhenStartAlreadyArrived() {
        LocalDateTime inicio = now().minusMinutes(10);
        Cita cita = cita(707L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, inicio, inicio.plusMinutes(45));
        when(citaRepository.findByIdForUpdate(707L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(707L)))
                .thenReturn(List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45)));

        CitaDetalleResponse response = service.marcarNoAtendida(707L, new MarcarNoAtendidaRequest("Paciente ausente"));

        assertThat(response.estado()).isEqualTo(EstadoCita.NO_ATENDIDA);
        assertThat(cita.getMotivoNoAtencion()).isEqualTo("Paciente ausente");
    }

    @Test
    void shouldRejectNoAtendidaBeforeStartOrFromTerminalState() {
        Cita futura = cita(708L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 15, 0), futureAt(2, 15, 45));
        Cita terminal = cita(709L, EstadoCita.CANCELADA, TipoCita.MEDICA, 20L, pastAt(1, 11, 0), pastAt(1, 11, 45));
        when(citaRepository.findByIdForUpdate(708L)).thenReturn(Optional.of(futura));
        when(citaRepository.findByIdForUpdate(709L)).thenReturn(Optional.of(terminal));

        assertThatThrownBy(() -> service.marcarNoAtendida(708L, new MarcarNoAtendidaRequest("No llegó")))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("inicio");
        assertThatThrownBy(() -> service.marcarNoAtendida(709L, new MarcarNoAtendidaRequest("No llegó")))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("estado");
    }

    @Test
    void shouldMarkConfirmedAppointmentAsAtendidaForAdministrador() {
        LocalDateTime inicio = now().minusMinutes(20);
        Cita cita = cita(710L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, inicio, inicio.plusMinutes(45));
        when(citaRepository.findByIdForUpdate(710L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(710L)))
                .thenReturn(List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45)));
        when(usuarioRolRepository.findActivosByUsuarioId(1L)).thenReturn(roles(NombreRol.ADMINISTRADOR));

        CitaDetalleResponse response = service.marcarAtendida(710L, 1L);

        assertThat(response.estado()).isEqualTo(EstadoCita.ATENDIDA);
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void shouldMarkConfirmedAppointmentAsAtendidaForAssignedCompatibleProfessional() {
        LocalDateTime inicio = now().minusMinutes(15);
        Cita cita = cita(711L, EstadoCita.CONFIRMADA, TipoCita.PELUQUERIA, 21L, inicio, inicio.plusMinutes(35));
        when(citaRepository.findByIdForUpdate(711L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(711L)))
                .thenReturn(List.of(citaServicio(cita, 31L, TipoServicio.PELUQUERIA, new BigDecimal("65.50"), 35)));
        when(usuarioRolRepository.findActivosByUsuarioId(21L)).thenReturn(roles(NombreRol.PELUQUERO));

        CitaDetalleResponse response = service.marcarAtendida(711L, 21L);

        assertThat(response.estado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void shouldRejectAtendidaForRecepcionistaOrForeignProfessional() {
        LocalDateTime inicio = now().minusMinutes(10);
        Cita citaRecepcion = cita(712L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, inicio, inicio.plusMinutes(45));
        Cita citaAjena = cita(713L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, inicio, inicio.plusMinutes(45));
        when(citaRepository.findByIdForUpdate(712L)).thenReturn(Optional.of(citaRecepcion));
        when(citaRepository.findByIdForUpdate(713L)).thenReturn(Optional.of(citaAjena));
        when(usuarioRolRepository.findActivosByUsuarioId(2L)).thenReturn(roles(NombreRol.RECEPCIONISTA));
        when(usuarioRolRepository.findActivosByUsuarioId(99L)).thenReturn(roles(NombreRol.VETERINARIO));

        assertThatThrownBy(() -> service.marcarAtendida(712L, 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso");
        assertThatThrownBy(() -> service.marcarAtendida(713L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso");
    }

    @Test
    void shouldRejectAtendidaBeforeStartOrOutsideConfirmedState() {
        Cita futura = cita(714L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 16, 0), futureAt(2, 16, 45));
        Cita pendiente = cita(715L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 20L, now().minusMinutes(10), now().plusMinutes(35));
        when(citaRepository.findByIdForUpdate(714L)).thenReturn(Optional.of(futura));
        when(citaRepository.findByIdForUpdate(715L)).thenReturn(Optional.of(pendiente));
        when(usuarioRolRepository.findActivosByUsuarioId(1L)).thenReturn(roles(NombreRol.ADMINISTRADOR));

        assertThatThrownBy(() -> service.marcarAtendida(714L, 1L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("inicio");
        assertThatThrownBy(() -> service.marcarAtendida(715L, 1L))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("estado");
    }

    @Test
    void shouldAcceptAtendidaWhenStartIsASecondBeforeNowWithinSameMinute() {
        LocalDateTime inicio = now().minusSeconds(1);
        Cita cita = cita(719L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, inicio, inicio.plusMinutes(45));
        when(citaRepository.findByIdForUpdate(719L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(719L)))
                .thenReturn(List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45)));
        when(usuarioRolRepository.findActivosByUsuarioId(1L)).thenReturn(roles(NombreRol.ADMINISTRADOR));

        CitaDetalleResponse response = service.marcarAtendida(719L, 1L);

        assertThat(response.estado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void shouldAcceptNoAtendidaWhenStartEqualsNow() {
        LocalDateTime inicio = now();
        Cita cita = cita(720L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, inicio, inicio.plusMinutes(45));
        when(citaRepository.findByIdForUpdate(720L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(720L)))
                .thenReturn(List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45)));

        CitaDetalleResponse response = service.marcarNoAtendida(720L, new MarcarNoAtendidaRequest("No llego"));

        assertThat(response.estado()).isEqualTo(EstadoCita.NO_ATENDIDA);
    }

    @Test
    void shouldReprogramAppointmentUsingHistoricalDurationsAndLockingCitaThenTrabajador() {
        LocalDateTime inicioOriginal = futureAt(2, 9, 0);
        LocalDateTime nuevoInicio = futureAt(3, 11, 30);
        Cita cita = cita(716L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, inicioOriginal, inicioOriginal.plusMinutes(45));
        List<CitaServicio> servicios = List.of(
                citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45),
                citaServicio(cita, 32L, TipoServicio.MEDICO, BigDecimal.ZERO, 20));
        Usuario nuevoTrabajador = usuarioActivo(25L, "Rosa", "Mendoza");
        when(citaRepository.findByIdForUpdate(716L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(716L))).thenReturn(servicios);
        when(usuarioRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(nuevoTrabajador));
        when(usuarioRolRepository.findActivosByUsuarioId(25L)).thenReturn(roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(25L, nuevoInicio, nuevoInicio.plusMinutes(65)))
                .thenReturn(disponible(25L, nuevoInicio, nuevoInicio.plusMinutes(65)));
        when(citaRepository.existsSolapamiento(25L, EstadoAgendaCita.ocupantes(), nuevoInicio, nuevoInicio.plusMinutes(65), 716L))
                .thenReturn(false);

        CitaDetalleResponse response = service.reprogramar(716L, new ReprogramarCitaRequest(25L, nuevoInicio));

        assertThat(cita.getTrabajadorAsignado()).isSameAs(nuevoTrabajador);
        assertThat(cita.getFechaHoraInicio()).isEqualTo(nuevoInicio);
        assertThat(cita.getFechaHoraFin()).isEqualTo(nuevoInicio.plusMinutes(65));
        assertThat(cita.getMotivoConsulta()).isEqualTo("Motivo 716");
        assertThat(response.fechaHoraFin()).isEqualTo(nuevoInicio.plusMinutes(65));
        assertThat(response.servicios())
                .extracting(servicio -> servicio.id(), servicio -> servicio.duracionAplicadaMinutos())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(30L, 45),
                        org.assertj.core.groups.Tuple.tuple(32L, 20));

        InOrder inOrder = inOrder(citaRepository, usuarioRepository, disponibilidadTrabajadorService);
        inOrder.verify(citaRepository).findByIdForUpdate(716L);
        inOrder.verify(usuarioRepository).findByIdForUpdate(25L);
        inOrder.verify(disponibilidadTrabajadorService).consultar(25L, nuevoInicio, nuevoInicio.plusMinutes(65));
        verify(citaRepository).existsSolapamiento(25L, EstadoAgendaCita.ocupantes(), nuevoInicio, nuevoInicio.plusMinutes(65), 716L);
    }

    @Test
    void shouldRejectReprogramWhenRangeCompatibilityAvailabilityOrOverlapFail() {
        LocalDateTime inicioOriginal = futureAt(2, 8, 0);
        Cita cita = cita(717L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 20L, inicioOriginal, inicioOriginal.plusMinutes(45));
        List<CitaServicio> servicios = List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45));
        Usuario peluquero = usuarioActivo(26L, "Luis", "Peine");
        Usuario veterinario = usuarioActivo(27L, "Cora", "Campos");
        when(citaRepository.findByIdForUpdate(717L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(717L))).thenReturn(servicios);
        when(usuarioRepository.findByIdForUpdate(26L)).thenReturn(Optional.of(peluquero));
        when(usuarioRepository.findByIdForUpdate(27L)).thenReturn(Optional.of(veterinario));
        when(usuarioRolRepository.findActivosByUsuarioId(26L)).thenReturn(roles(NombreRol.PELUQUERO));
        when(usuarioRolRepository.findActivosByUsuarioId(27L)).thenReturn(roles(NombreRol.VETERINARIO));
        LocalDateTime cruceDia = futureAt(2, 23, 30);
        LocalDateTime sinHorario = futureAt(3, 7, 0);
        LocalDateTime conSolape = futureAt(4, 9, 0);
        when(disponibilidadTrabajadorService.consultar(27L, sinHorario, sinHorario.plusMinutes(45)))
                .thenReturn(new DisponibilidadTrabajadorResponse(27L, sinHorario, sinHorario.plusMinutes(45), false, "Fuera del horario laboral."));
        when(disponibilidadTrabajadorService.consultar(27L, conSolape, conSolape.plusMinutes(45)))
                .thenReturn(disponible(27L, conSolape, conSolape.plusMinutes(45)));
        when(citaRepository.existsSolapamiento(27L, EstadoAgendaCita.ocupantes(), conSolape, conSolape.plusMinutes(45), 717L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.reprogramar(717L, new ReprogramarCitaRequest(20L, now().minusMinutes(1))))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("futuro");
        assertThatThrownBy(() -> service.reprogramar(717L, new ReprogramarCitaRequest(27L, cruceDia)))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("dia");
        assertThatThrownBy(() -> service.reprogramar(717L, new ReprogramarCitaRequest(26L, futureAt(3, 10, 0))))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("compatible");
        assertThatThrownBy(() -> service.reprogramar(717L, new ReprogramarCitaRequest(27L, sinHorario)))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("horario");
        assertThatThrownBy(() -> service.reprogramar(717L, new ReprogramarCitaRequest(27L, conSolape)))
                .isInstanceOf(CitaConflictException.class)
                .hasMessageContaining("solapa");
    }

    @Test
    void shouldRejectReprogramWhenStartIsASecondBeforeNowWithinSameMinute() {
        LocalDateTime inicioOriginal = futureAt(2, 8, 0);
        Cita cita = cita(721L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 20L, inicioOriginal, inicioOriginal.plusMinutes(45));
        when(citaRepository.findByIdForUpdate(721L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(721L)))
                .thenReturn(List.of(citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("80.00"), 45)));

        assertThatThrownBy(() -> service.reprogramar(721L, new ReprogramarCitaRequest(20L, now().minusSeconds(1))))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void shouldCombineBaseAvailabilityAndAppointmentOverlap() {
        LocalDateTime inicio = futureAt(5, 10, 0);
        LocalDateTime fin = inicio.plusMinutes(45);
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioActivo(20L, "Julio", "Paredes")));
        when(usuarioRolRepository.findActivosByUsuarioId(20L)).thenReturn(roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, fin))
                .thenReturn(disponible(20L, inicio, fin));
        when(citaRepository.existsSolapamiento(20L, EstadoAgendaCita.ocupantes(), inicio, fin, null))
                .thenReturn(true);

        DisponibilidadCitaResponse response = service.consultarDisponibilidad(20L, TipoCita.MEDICA, inicio, fin);

        assertThat(response.trabajadorId()).isEqualTo(20L);
        assertThat(response.tipoCita()).isEqualTo(TipoCita.MEDICA);
        assertThat(response.disponibilidadBase()).isTrue();
        assertThat(response.tieneCitaSolapada()).isTrue();
        assertThat(response.disponible()).isFalse();
        assertThat(response.motivo()).contains("solapa");
    }

    @Test
    void shouldReturnAvailableWhenBaseAvailabilityPassesAndNoActiveOverlapExists() {
        LocalDateTime inicio = futureAt(6, 12, 0);
        LocalDateTime fin = inicio.plusMinutes(35);
        when(usuarioRepository.findById(21L)).thenReturn(Optional.of(usuarioActivo(21L, "Lucia", "Cortez")));
        when(usuarioRolRepository.findActivosByUsuarioId(21L)).thenReturn(roles(NombreRol.PELUQUERO));
        when(disponibilidadTrabajadorService.consultar(21L, inicio, fin))
                .thenReturn(disponible(21L, inicio, fin));
        when(citaRepository.existsSolapamiento(21L, EstadoAgendaCita.ocupantes(), inicio, fin, null))
                .thenReturn(false);

        DisponibilidadCitaResponse response = service.consultarDisponibilidad(21L, TipoCita.PELUQUERIA, inicio, fin);

        assertThat(response.disponibilidadBase()).isTrue();
        assertThat(response.tieneCitaSolapada()).isFalse();
        assertThat(response.disponible()).isTrue();
        assertThat(response.motivo()).isEqualTo("Disponible");
    }

    @Test
    void shouldReturnBaseAvailabilityFailureWithoutOverlapFlag() {
        LocalDateTime inicio = futureAt(7, 8, 0);
        LocalDateTime fin = inicio.plusMinutes(45);
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioActivo(20L, "Julio", "Paredes")));
        when(usuarioRolRepository.findActivosByUsuarioId(20L)).thenReturn(roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, fin))
                .thenReturn(new DisponibilidadTrabajadorResponse(20L, inicio, fin, false, "Existe una indisponibilidad en el intervalo."));

        DisponibilidadCitaResponse response = service.consultarDisponibilidad(20L, TipoCita.MEDICA, inicio, fin);

        assertThat(response.disponibilidadBase()).isFalse();
        assertThat(response.tieneCitaSolapada()).isFalse();
        assertThat(response.disponible()).isFalse();
        assertThat(response.motivo()).contains("indisponibilidad");
        verify(citaRepository, never()).existsSolapamiento(20L, EstadoAgendaCita.ocupantes(), inicio, fin, null);
    }

    @Test
    void shouldRejectDisponibilidadWhenStartIsASecondBeforeNowWithinSameMinute() {
        LocalDateTime inicio = now().minusSeconds(1);
        LocalDateTime fin = now().plusMinutes(30);

        assertThatThrownBy(() -> service.consultarDisponibilidad(20L, TipoCita.MEDICA, inicio, fin))
                .isInstanceOf(CitaBadRequestException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void shouldAllowAssignedCompatibleProfessionalToConfirmUnderAppointmentLock() {
        Cita cita = cita(70L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 20L, futureAt(2, 9, 0), futureAt(2, 9, 30));
        when(citaRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(70L))).thenReturn(List.of());

        CitaDetalleResponse response = service.confirmar(70L, 20L, Set.of(NombreRol.VETERINARIO));

        assertThat(response.estado()).isEqualTo(EstadoCita.CONFIRMADA);
        verify(citaRepository).findByIdForUpdate(70L);
    }

    @Test
    void shouldRejectProfessionalOperatingAnotherWorkersAppointmentAfterLock() {
        Cita cita = cita(71L, EstadoCita.PENDIENTE, TipoCita.MEDICA, 21L, futureAt(2, 9, 0), futureAt(2, 9, 30));
        when(citaRepository.findByIdForUpdate(71L)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> service.confirmar(71L, 20L, Set.of(NombreRol.VETERINARIO)))
                .isInstanceOf(AccessDeniedException.class);

        verify(citaRepository).findByIdForUpdate(71L);
        verifyNoInteractions(citaServicioRepository);
    }

    @Test
    void shouldForceProfessionalAsTargetWorkerWhenReprogrammingOwnAppointment() {
        LocalDateTime inicio = futureAt(4, 10, 0);
        Cita cita = cita(72L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, futureAt(2, 9, 0), futureAt(2, 9, 30));
        CitaServicio snapshot = citaServicio(cita, 30L, TipoServicio.MEDICO, new BigDecimal("50.00"), 30);
        Usuario profesional = usuarioActivo(20L, "Vet", "Propio");
        when(citaRepository.findByIdForUpdate(72L)).thenReturn(Optional.of(cita));
        when(citaServicioRepository.findByCitaIdIn(List.of(72L))).thenReturn(List.of(snapshot));
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(profesional));
        when(usuarioRolRepository.findActivosByUsuarioId(20L)).thenReturn(roles(NombreRol.VETERINARIO));
        when(disponibilidadTrabajadorService.consultar(20L, inicio, inicio.plusMinutes(30)))
                .thenReturn(disponible(20L, inicio, inicio.plusMinutes(30)));
        when(citaRepository.existsSolapamiento(
                20L, EstadoAgendaCita.ocupantes(), inicio, inicio.plusMinutes(30), 72L)).thenReturn(false);

        CitaDetalleResponse response = service.reprogramar(
                72L,
                new ReprogramarCitaRequest(999L, inicio),
                20L,
                Set.of(NombreRol.VETERINARIO));

        assertThat(response.trabajador().id()).isEqualTo(20L);
        verify(usuarioRepository).findByIdForUpdate(20L);
        verify(usuarioRepository, never()).findByIdForUpdate(999L);
    }

    @Test
    void shouldRejectReceptionistMarkingAppointmentAsAttended() {
        Cita cita = cita(73L, EstadoCita.CONFIRMADA, TipoCita.MEDICA, 20L, pastAt(1, 9, 0), pastAt(1, 9, 30));
        when(citaRepository.findByIdForUpdate(73L)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> service.marcarAtendida(73L, 2L, Set.of(NombreRol.RECEPCIONISTA)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldRejectProfessionalCheckingAnotherWorkersAvailability() {
        LocalDateTime inicio = futureAt(3, 10, 0);

        assertThatThrownBy(() -> service.consultarDisponibilidad(
                21L,
                TipoCita.MEDICA,
                inicio,
                inicio.plusMinutes(30),
                20L,
                Set.of(NombreRol.VETERINARIO)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(disponibilidadTrabajadorService);
    }

    private void stubMascotaRegistradorServiciosYTrabajador(
            Mascota mascota,
            Usuario registrador,
            List<Servicio> servicios,
            Usuario trabajador,
            List<UsuarioRol> roles) {
        when(mascotaRepository.findById(mascota.getId())).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(registrador.getId())).thenReturn(Optional.of(registrador));
        when(servicioRepository.findAllById(idsDeServicios(servicios))).thenReturn(servicios);
        when(usuarioRepository.findByIdForUpdate(trabajador.getId())).thenReturn(Optional.of(trabajador));
        when(usuarioRolRepository.findActivosByUsuarioId(trabajador.getId())).thenReturn(roles);
    }

    @SuppressWarnings("unchecked")
    private List<CitaServicio> capturarCitaServiciosGuardados() {
        ArgumentCaptor<List<CitaServicio>> captor = ArgumentCaptor.forClass(List.class);
        verify(citaServicioRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Set<Long> idsDeServicios(List<Servicio> servicios) {
        return servicios.stream()
                .map(Servicio::getId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private List<UsuarioRol> roles(NombreRol... nombres) {
        List<UsuarioRol> roles = new ArrayList<>();
        for (NombreRol nombre : nombres) {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            rol.setActivo(Boolean.TRUE);

            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setRol(rol);
            roles.add(usuarioRol);
        }
        return roles;
    }

    private CrearCitaRequest requestMedicaBasica(LocalDateTime inicio) {
        return new CrearCitaRequest(
                10L,
                20L,
                inicio,
                List.of(new ServicioCitaRequest(30L, null)),
                "Control general",
                null);
    }

    private CitaListadoFiltroRequest filtroListado(int page, int size, List<String> sort) {
        return new CitaListadoFiltroRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                page,
                size,
                sort);
    }

    private DisponibilidadTrabajadorResponse disponible(Long trabajadorId, LocalDateTime inicio, LocalDateTime fin) {
        return new DisponibilidadTrabajadorResponse(trabajadorId, inicio, fin, true, "Disponible");
    }

    @SuppressWarnings("unchecked")
    private void assertThatTrabajadorIdFiltrado(Specification<Cita> specification, Long expectedTrabajadorId) {
        Root<Cita> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Join<Object, Object> trabajador = org.mockito.Mockito.mock(Join.class);
        Path<Object> trabajadorIdPath = org.mockito.Mockito.mock(Path.class);
        Predicate conjunction = org.mockito.Mockito.mock(Predicate.class);
        Predicate trabajadorPredicate = org.mockito.Mockito.mock(Predicate.class);

        when(root.join("trabajadorAsignado", JoinType.INNER)).thenReturn(trabajador);
        when(trabajador.get("id")).thenReturn(trabajadorIdPath);
        when(criteriaBuilder.conjunction()).thenReturn(conjunction);
        when(criteriaBuilder.equal(trabajadorIdPath, expectedTrabajadorId)).thenReturn(trabajadorPredicate);
        org.mockito.Mockito.doReturn(trabajadorPredicate)
                .when(criteriaBuilder)
                .and(
                        org.mockito.ArgumentMatchers.any(Predicate.class),
                        org.mockito.ArgumentMatchers.any(Predicate.class));

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(trabajadorIdPath, expectedTrabajadorId);
        verify(criteriaBuilder, never()).equal(trabajadorIdPath, 999L);
    }

    private Cita cita(
            Long id,
            EstadoCita estado,
            TipoCita tipoCita,
            Long trabajadorId,
            LocalDateTime inicio,
            LocalDateTime fin) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setEstado(estado);
        cita.setTipoCita(tipoCita);
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(fin);
        cita.setMotivoConsulta("Motivo " + id);
        cita.setObservaciones("Observacion " + id);
        cita.setMascota(mascotaActiva(10L + id, clienteActivo(100L + id)));
        cita.setTrabajadorAsignado(usuarioActivo(trabajadorId, "Trabajador", "Asignado"));
        cita.setRegistradoPor(usuarioActivo(40L + id, "Recepcion", "Registro"));
        return cita;
    }

    private CitaServicio citaServicio(
            Cita cita,
            Long servicioId,
            TipoServicio tipoServicio,
            BigDecimal precioAplicado,
            Integer duracionAplicadaMinutos) {
        CitaServicio citaServicio = new CitaServicio();
        citaServicio.setCita(cita);
        citaServicio.setServicio(servicio(servicioId, tipoServicio, precioAplicado, duracionAplicadaMinutos, true));
        citaServicio.setPrecioAplicado(precioAplicado);
        citaServicio.setDuracionAplicadaMinutos(duracionAplicadaMinutos);
        return citaServicio;
    }

    private Mascota mascotaActiva(Long id, Cliente cliente) {
        Mascota mascota = new Mascota();
        mascota.setId(id);
        mascota.setCliente(cliente);
        mascota.setNombre("Luna");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setActivo(Boolean.TRUE);
        return mascota;
    }

    private Cliente clienteActivo(Long id) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setPrimerNombre("Maria");
        cliente.setPrimerApellido("Torres");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("12345678");
        cliente.setTelefono("999888777");
        cliente.setCorreo("maria@test.dev");
        cliente.setActivo(Boolean.TRUE);
        return cliente;
    }

    private Usuario usuarioActivo(Long id, String primerNombre, String primerApellido) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setPrimerNombre(primerNombre);
        usuario.setPrimerApellido(primerApellido);
        usuario.setCorreo((primerNombre + "." + primerApellido + "@test.dev").toLowerCase());
        usuario.setPasswordHash("hash");
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(0);
        return usuario;
    }

    private Servicio servicio(Long id, TipoServicio tipoServicio, BigDecimal precioBase, Integer duracion, boolean activo) {
        Servicio servicio = new Servicio();
        servicio.setId(id);
        servicio.setNombre("Servicio " + id);
        servicio.setTipoServicio(tipoServicio);
        servicio.setPrecioBase(precioBase);
        servicio.setDuracionMinutos(duracion);
        servicio.setActivo(activo);
        return servicio;
    }

    private PrecioServicioTamano tarifa(Long id, Servicio servicio, BigDecimal precio, Integer duracion, boolean activo) {
        PrecioServicioTamano tarifa = new PrecioServicioTamano();
        tarifa.setId(id);
        tarifa.setServicio(servicio);
        tarifa.setTamanoMascota(TamanoMascota.MEDIANO);
        tarifa.setPrecio(precio);
        tarifa.setDuracionMinutos(duracion);
        tarifa.setActivo(activo);
        return tarifa;
    }

    private LocalDateTime futureAt(int days, int hour, int minute) {
        return now().plusDays(days).withHour(hour).withMinute(minute);
    }

    private LocalDateTime pastAt(int days, int hour, int minute) {
        return now().minusDays(days).withHour(hour).withMinute(minute);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(fixedClock);
    }
}
