package com.veterinaria.backend.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.jpa.domain.Specification.where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.veterinaria.backend.cita.entity.Cita;
import com.veterinaria.backend.cita.entity.CitaServicio;
import com.veterinaria.backend.cita.enums.EstadoCita;
import com.veterinaria.backend.cita.enums.TipoCita;
import com.veterinaria.backend.cita.repository.CitaRepository;
import com.veterinaria.backend.cita.repository.CitaServicioRepository;
import com.veterinaria.backend.cita.repository.CitaSpecifications;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.cliente.enums.TipoDocumento;
import com.veterinaria.backend.cliente.repository.ClienteRepository;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.enums.EspecieMascota;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TamanoMascota;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.repository.PrecioServicioTamanoRepository;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CitaRepositoryIntegrationTest extends PostgreSqlContainerConfiguration {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private CitaServicioRepository citaServicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private PrecioServicioTamanoRepository precioServicioTamanoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate requiresNewTx;
    private PersistenceUnitUtil persistenceUnitUtil;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        requiresNewTx = new TransactionTemplate(transactionManager);
        requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        persistenceUnitUtil = entityManagerFactory.getPersistenceUnitUtil();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldAcquirePessimisticWriteLocksAndReleaseThemForUsuarioAndCita() throws Exception {
        LockFixture fixture = requiresNewTx.execute(status -> {
            Usuario trabajador = createUsuario("Trabajador", "Lock");
            Usuario registrador = createUsuario("Registrador", "Lock");
            Cliente cliente = createCliente("70000001", "Maria", "Locks");
            Mascota mascota = createMascota(cliente, "Locky");
            Cita cita = createCita(
                    mascota,
                    trabajador,
                    registrador,
                    TipoCita.MEDICA,
                    EstadoCita.PENDIENTE,
                    now().plusDays(2).withHour(9).withMinute(0),
                    now().plusDays(2).withHour(10).withMinute(0),
                    "Control lock",
                    "Sin observaciones");
            entityManager.flush();
            return new LockFixture(trabajador.getId(), registrador.getId(), cliente.getId(), mascota.getId(), cita.getId());
        });

        try {
            assertPessimisticWriteLock(
                    () -> assertThat(usuarioRepository.findByIdForUpdate(fixture.trabajadorId())).isPresent(),
                    () -> assertThat(usuarioRepository.findByIdForUpdate(fixture.trabajadorId())).isPresent());

            assertPessimisticWriteLock(
                    () -> assertThat(citaRepository.findByIdForUpdate(fixture.citaId())).isPresent(),
                    () -> assertThat(citaRepository.findByIdForUpdate(fixture.citaId())).isPresent());
        } finally {
            requiresNewTx.executeWithoutResult(status -> {
                citaRepository.deleteById(fixture.citaId());
                mascotaRepository.deleteById(fixture.mascotaId());
                clienteRepository.deleteById(fixture.clienteId());
                usuarioRepository.deleteById(fixture.registradorId());
                usuarioRepository.deleteById(fixture.trabajadorId());
            });
        }
    }

    @Test
    void shouldDetectOverlapRespectingOccupyingStatesExclusionAndContiguity() {
        Usuario trabajador = createUsuario("Julio", "Agenda");
        Usuario registrador = createUsuario("Paola", "Agenda");
        Cliente cliente = createCliente("70000002", "Maria", "Agenda");
        Mascota mascota = createMascota(cliente, "Luna Agenda");
        LocalDateTime inicioBase = now().plusDays(3).withHour(10).withMinute(0);

        Cita pendiente = createCita(
                mascota,
                trabajador,
                registrador,
                TipoCita.MEDICA,
                EstadoCita.PENDIENTE,
                inicioBase,
                inicioBase.plusHours(1),
                "Pendiente ocupante",
                null);

        createCita(
                mascota,
                trabajador,
                registrador,
                TipoCita.MEDICA,
                EstadoCita.CANCELADA,
                inicioBase.plusMinutes(10),
                inicioBase.plusMinutes(40),
                null,
                null,
                null,
                "Motivo de cancelacion");

        assertThat(citaRepository.existsSolapamiento(
                trabajador.getId(),
                Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA),
                inicioBase.plusMinutes(20),
                inicioBase.plusMinutes(50),
                null)).isTrue();

        assertThat(citaRepository.existsSolapamiento(
                trabajador.getId(),
                Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA),
                inicioBase.plusHours(1),
                inicioBase.plusHours(1).plusMinutes(30),
                null)).isFalse();

        assertThat(citaRepository.existsSolapamiento(
                trabajador.getId(),
                Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA),
                inicioBase.plusMinutes(20),
                inicioBase.plusMinutes(50),
                pendiente.getId())).isFalse();

        assertThat(citaRepository.existsSolapamiento(
                trabajador.getId(),
                Set.of(EstadoCita.CONFIRMADA),
                inicioBase.plusMinutes(20),
                inicioBase.plusMinutes(50),
                null)).isFalse();
    }

    @Test
    void shouldFilterBySpecificationsAndLoadCreationLookupsWithToOneGraphs() {
        Usuario trabajadorObjetivo = createUsuario("Julio", "Paredes");
        Usuario otroTrabajador = createUsuario("Lucia", "Cortez");
        Usuario registrador = createUsuario("Paola", "Recepcion");
        Cliente clienteObjetivo = createCliente("70000003", "Maria", "Torres");
        Cliente otroCliente = createCliente("70000004", "Carlos", "Lopez");
        Mascota mascotaObjetivo = createMascota(clienteObjetivo, "Luna Repo");
        Mascota otraMascota = createMascota(otroCliente, "Rocky Repo");
        Servicio servicio = createServicio("Consulta repo", TipoServicio.MEDICO, 45, new BigDecimal("80.00"));
        PrecioServicioTamano precioServicio = createPrecioServicioTamano(servicio, TamanoMascota.MEDIANO, new BigDecimal("95.00"), 60);
        LocalDateTime inicioBase = now().plusDays(4).withHour(8).withMinute(0);

        Cita citaObjetivo = createCita(
                mascotaObjetivo,
                trabajadorObjetivo,
                registrador,
                TipoCita.MEDICA,
                EstadoCita.CONFIRMADA,
                inicioBase,
                inicioBase.plusMinutes(45),
                "Control anual",
                "Buscar cliente");

        createCitaServicio(citaObjetivo, servicio, precioServicio, new BigDecimal("95.00"), 60);

        createCita(
                otraMascota,
                otroTrabajador,
                registrador,
                TipoCita.PELUQUERIA,
                EstadoCita.PENDIENTE,
                inicioBase.plusDays(1),
                inicioBase.plusDays(1).plusMinutes(30),
                "Bano",
                "No coincide");

        Mascota mascotaCargada = mascotaRepository.findById(mascotaObjetivo.getId()).orElseThrow();
        PrecioServicioTamano precioCargado = precioServicioTamanoRepository.findById(precioServicio.getId()).orElseThrow();
        entityManager.clear();

        assertThat(persistenceUnitUtil.isLoaded(mascotaCargada, "cliente")).isTrue();
        assertThat(mascotaCargada.getCliente().getNumeroDocumento()).isEqualTo("70000003");
        assertThat(persistenceUnitUtil.isLoaded(precioCargado, "servicio")).isTrue();
        assertThat(precioCargado.getServicio().getNombre()).isEqualTo(servicio.getNombre());

        Page<Cita> page = citaRepository.findAll(
                where(CitaSpecifications.fechaHoraInicioDesde(inicioBase.minusMinutes(1)))
                        .and(CitaSpecifications.fechaHoraInicioHasta(inicioBase.plusHours(1)))
                        .and(CitaSpecifications.estadoEquals(EstadoCita.CONFIRMADA))
                        .and(CitaSpecifications.tipoCitaEquals(TipoCita.MEDICA))
                        .and(CitaSpecifications.clienteIdEquals(clienteObjetivo.getId()))
                        .and(CitaSpecifications.mascotaIdEquals(mascotaObjetivo.getId()))
                        .and(CitaSpecifications.trabajadorIdEquals(trabajadorObjetivo.getId()))
                        .and(CitaSpecifications.search("70000003")),
                PageRequest.of(0, 10, Sort.by("fechaHoraInicio").ascending()));

        assertThat(page.getContent()).extracting(Cita::getId).containsExactly(citaObjetivo.getId());

        Page<Cita> searchPorMascota = citaRepository.findAll(
                where(CitaSpecifications.search("luna repo")),
                PageRequest.of(0, 10, Sort.by("fechaHoraInicio").ascending()));

        assertThat(searchPorMascota.getContent()).extracting(Cita::getId).contains(citaObjetivo.getId());
    }

    @Test
    void shouldLoadPagedCitasWithToOneGraphAndBatchServicesWithoutNPlusOneQueries() {
        Usuario trabajador = createUsuario("Julio", "Batch");
        Usuario registrador = createUsuario("Paola", "Batch");
        Cliente cliente = createCliente("70000005", "Maria", "Batch");
        Mascota mascota = createMascota(cliente, "Luna Batch");
        Servicio consulta = createServicio("Consulta batch", TipoServicio.MEDICO, 45, new BigDecimal("70.00"));
        Servicio bano = createServicio("Bano batch", TipoServicio.PELUQUERIA, 30, new BigDecimal("40.00"));
        PrecioServicioTamano tarifaBano = createPrecioServicioTamano(bano, TamanoMascota.MEDIANO, new BigDecimal("55.00"), 35);
        LocalDateTime inicioBase = now().plusDays(5).withHour(9).withMinute(0);

        Cita primera = createCita(
                mascota,
                trabajador,
                registrador,
                TipoCita.MEDICA,
                EstadoCita.CONFIRMADA,
                inicioBase,
                inicioBase.plusMinutes(45),
                "Primera cita",
                null);

        Cita segunda = createCita(
                mascota,
                trabajador,
                registrador,
                TipoCita.PELUQUERIA,
                EstadoCita.PENDIENTE,
                inicioBase.plusHours(2),
                inicioBase.plusHours(2).plusMinutes(35),
                "Segunda cita",
                "Con tarifa");

        createCita(
                mascota,
                trabajador,
                registrador,
                TipoCita.MEDICA,
                EstadoCita.PENDIENTE,
                inicioBase.plusHours(4),
                inicioBase.plusHours(4).plusMinutes(45),
                "Tercera cita",
                null);

        createCitaServicio(primera, consulta, null, new BigDecimal("70.00"), 45);
        createCitaServicio(segunda, bano, tarifaBano, new BigDecimal("55.00"), 35);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<Cita> page = citaRepository.findAll(
                where(CitaSpecifications.trabajadorIdEquals(trabajador.getId())),
                PageRequest.of(0, 2, Sort.by("fechaHoraInicio").ascending()));

        List<Cita> citas = new ArrayList<>(page.getContent());
        List<Long> citaIds = citas.stream().map(Cita::getId).toList();
        List<CitaServicio> citaServicios = citaServicioRepository.findByCitaIdIn(citaIds);
        entityManager.clear();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3L);
        assertThat(citas).hasSize(2);
        assertThat(citaServicios).hasSize(2);

        Cita citaCargada = citas.get(0);
        assertThat(persistenceUnitUtil.isLoaded(citaCargada, "mascota")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(citaCargada.getMascota(), "cliente")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(citaCargada, "trabajadorAsignado")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(citaCargada, "registradoPor")).isTrue();
        assertThat(citaCargada.getMascota().getCliente().getPrimerNombre()).isEqualTo("Maria");
        assertThat(citaCargada.getTrabajadorAsignado().getPrimerApellido()).isEqualTo("Batch");
        assertThat(citaCargada.getRegistradoPor().getPrimerNombre()).isEqualTo("Paola");

        CitaServicio conTarifa = citaServicios.stream()
                .filter(item -> item.getPrecioServicioTamano() != null)
                .findFirst()
                .orElseThrow();

        assertThat(persistenceUnitUtil.isLoaded(conTarifa, "servicio")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(conTarifa, "precioServicioTamano")).isTrue();
        assertThat(conTarifa.getServicio().getNombre()).isEqualTo(bano.getNombre());
        assertThat(conTarifa.getPrecioServicioTamano().getTamanoMascota()).isEqualTo(TamanoMascota.MEDIANO);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3L);
    }

    @Test
    void shouldReturnServicesInPersistedRequestOrder() {
        Usuario trabajador = createUsuario("Orden", "Trabajador");
        Usuario registrador = createUsuario("Orden", "Registrador");
        Cliente cliente = createCliente("70000009", "Orden", "Cliente");
        Mascota mascota = createMascota(cliente, "Orden Mascota");
        Servicio primero = createServicio("Primero", TipoServicio.MEDICO, 20, new BigDecimal("30.00"));
        Servicio segundo = createServicio("Segundo", TipoServicio.MEDICO, 25, new BigDecimal("40.00"));
        LocalDateTime inicio = now().plusDays(5).withHour(10).withMinute(0);
        Cita cita = createCita(
                mascota,
                trabajador,
                registrador,
                TipoCita.MEDICA,
                EstadoCita.PENDIENTE,
                inicio,
                inicio.plusMinutes(45),
                "Orden estable",
                null);

        CitaServicio snapshotPrimero = createCitaServicio(cita, primero, null, new BigDecimal("30.00"), 20);
        CitaServicio snapshotSegundo = createCitaServicio(cita, segundo, null, new BigDecimal("40.00"), 25);
        entityManager.flush();
        entityManager.clear();

        List<CitaServicio> snapshots = citaServicioRepository.findByCitaIdIn(List.of(cita.getId()));

        assertThat(snapshots).extracting(CitaServicio::getId)
                .containsExactly(snapshotPrimero.getId(), snapshotSegundo.getId());
        assertThat(snapshots).extracting(item -> item.getServicio().getNombre())
                .containsExactly(primero.getNombre(), segundo.getNombre());
    }

    private void assertPessimisticWriteLock(Runnable firstLock, Runnable competingLock) throws Exception {
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> holdingTransaction = executor.submit(() -> requiresNewTx.executeWithoutResult(status -> {
            firstLock.run();
            lockHeld.countDown();
            await(releaseLock);
        }));

        try {
            assertThat(lockHeld.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> requiresNewTx.executeWithoutResult(status -> {
                entityManager.createNativeQuery("set local lock_timeout = '250ms'").executeUpdate();
                competingLock.run();
            })).satisfies(exception -> assertThat(rootCauseMessage(exception)).contains("lock timeout"));
        } finally {
            releaseLock.countDown();
            holdingTransaction.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();
        }

        requiresNewTx.executeWithoutResult(status -> competingLock.run());
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch timeout");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for lock release", exception);
        }
    }

    private Cliente createCliente(String numeroDocumento, String primerNombre, String primerApellido) {
        Cliente cliente = new Cliente();
        cliente.setPrimerNombre(primerNombre);
        cliente.setPrimerApellido(primerApellido);
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento(numeroDocumento);
        cliente.setActivo(Boolean.TRUE);
        return clienteRepository.save(cliente);
    }

    private Mascota createMascota(Cliente cliente, String nombre) {
        Mascota mascota = new Mascota();
        mascota.setCliente(cliente);
        mascota.setNombre(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setActivo(Boolean.TRUE);
        return mascotaRepository.save(mascota);
    }

    private Usuario createUsuario(String primerNombre, String primerApellido) {
        Usuario usuario = new Usuario();
        usuario.setPrimerNombre(primerNombre);
        usuario.setPrimerApellido(primerApellido);
        usuario.setCorreo(("task2-" + primerNombre + "." + primerApellido + "." + UUID.randomUUID() + "@test.dev").toLowerCase());
        usuario.setPasswordHash("hash");
        usuario.setActivo(Boolean.TRUE);
        usuario.setIntentosFallidos(0);
        return usuarioRepository.save(usuario);
    }

    private Servicio createServicio(String nombre, TipoServicio tipoServicio, Integer duracionMinutos, BigDecimal precioBase) {
        Servicio servicio = new Servicio();
        servicio.setNombre(nombre + "-" + UUID.randomUUID());
        servicio.setTipoServicio(tipoServicio);
        servicio.setDuracionMinutos(duracionMinutos);
        servicio.setPrecioBase(precioBase);
        servicio.setActivo(Boolean.TRUE);
        return servicioRepository.save(servicio);
    }

    private PrecioServicioTamano createPrecioServicioTamano(
            Servicio servicio,
            TamanoMascota tamanoMascota,
            BigDecimal precio,
            Integer duracionMinutos) {
        PrecioServicioTamano precioServicioTamano = new PrecioServicioTamano();
        precioServicioTamano.setServicio(servicio);
        precioServicioTamano.setTamanoMascota(tamanoMascota);
        precioServicioTamano.setPrecio(precio);
        precioServicioTamano.setDuracionMinutos(duracionMinutos);
        precioServicioTamano.setActivo(Boolean.TRUE);
        return precioServicioTamanoRepository.save(precioServicioTamano);
    }

    private Cita createCita(
            Mascota mascota,
            Usuario trabajador,
            Usuario registrador,
            TipoCita tipoCita,
            EstadoCita estado,
            LocalDateTime inicio,
            LocalDateTime fin,
            String motivoConsulta,
            String observaciones) {
        return createCita(mascota, trabajador, registrador, tipoCita, estado, inicio, fin, motivoConsulta, observaciones, null, null);
    }

    private Cita createCita(
            Mascota mascota,
            Usuario trabajador,
            Usuario registrador,
            TipoCita tipoCita,
            EstadoCita estado,
            LocalDateTime inicio,
            LocalDateTime fin,
            String motivoConsulta,
            String observaciones,
            String motivoNoAtencion,
            String motivoCancelacion) {
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setTrabajadorAsignado(trabajador);
        cita.setRegistradoPor(registrador);
        cita.setTipoCita(tipoCita);
        cita.setEstado(estado);
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(fin);
        cita.setMotivoConsulta(motivoConsulta);
        cita.setMotivoNoAtencion(motivoNoAtencion);
        cita.setMotivoCancelacion(motivoCancelacion);
        cita.setObservaciones(observaciones);
        return citaRepository.save(cita);
    }

    private CitaServicio createCitaServicio(
            Cita cita,
            Servicio servicio,
            PrecioServicioTamano precioServicioTamano,
            BigDecimal precioAplicado,
            Integer duracionAplicadaMinutos) {
        CitaServicio citaServicio = new CitaServicio();
        citaServicio.setCita(cita);
        citaServicio.setServicio(servicio);
        citaServicio.setPrecioServicioTamano(precioServicioTamano);
        citaServicio.setPrecioAplicado(precioAplicado);
        citaServicio.setDuracionAplicadaMinutos(duracionAplicadaMinutos);
        return citaServicioRepository.save(citaServicio);
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().withSecond(0).withNano(0);
    }

    private record LockFixture(
            Long trabajadorId,
            Long registradorId,
            Long clienteId,
            Long mascotaId,
            Long citaId) {
    }
}
