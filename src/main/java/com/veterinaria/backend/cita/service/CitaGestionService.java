package com.veterinaria.backend.cita.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

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
import com.veterinaria.backend.cita.repository.CitaSpecifications;
import com.veterinaria.backend.cliente.entity.Cliente;
import com.veterinaria.backend.horario.dto.DisponibilidadTrabajadorResponse;
import com.veterinaria.backend.horario.service.DisponibilidadTrabajadorService;
import com.veterinaria.backend.mascota.entity.Mascota;
import com.veterinaria.backend.mascota.exception.MascotaNoEncontradaException;
import com.veterinaria.backend.mascota.repository.MascotaRepository;
import com.veterinaria.backend.servicio.entity.PrecioServicioTamano;
import com.veterinaria.backend.servicio.entity.Servicio;
import com.veterinaria.backend.servicio.enums.TipoServicio;
import com.veterinaria.backend.servicio.exception.ServicioNoEncontradoException;
import com.veterinaria.backend.servicio.repository.PrecioServicioTamanoRepository;
import com.veterinaria.backend.servicio.repository.ServicioRepository;
import com.veterinaria.backend.usuario.entity.Usuario;
import com.veterinaria.backend.usuario.entity.UsuarioRol;
import com.veterinaria.backend.usuario.dto.PaginaResponse;
import com.veterinaria.backend.usuario.enums.NombreRol;
import com.veterinaria.backend.usuario.exception.UsuarioNoEncontradoException;
import com.veterinaria.backend.usuario.repository.UsuarioRepository;
import com.veterinaria.backend.usuario.repository.UsuarioRolRepository;

@Service
public class CitaGestionService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final List<String> DEFAULT_SORT = List.of("fechaHoraInicio,asc");
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "id", "id",
            "fechaHoraInicio", "fechaHoraInicio",
            "fechaHoraFin", "fechaHoraFin",
            "estado", "estado",
            "tipoCita", "tipoCita",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt");

    private final CitaRepository citaRepository;
    private final CitaServicioRepository citaServicioRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final ServicioRepository servicioRepository;
    private final PrecioServicioTamanoRepository precioServicioTamanoRepository;
    private final DisponibilidadTrabajadorService disponibilidadTrabajadorService;
    private final CitaMapper citaMapper;
    private final Clock clock;

    @Autowired
    public CitaGestionService(
            CitaRepository citaRepository,
            CitaServicioRepository citaServicioRepository,
            MascotaRepository mascotaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioRolRepository usuarioRolRepository,
            ServicioRepository servicioRepository,
            PrecioServicioTamanoRepository precioServicioTamanoRepository,
            DisponibilidadTrabajadorService disponibilidadTrabajadorService,
            CitaMapper citaMapper) {
        this(
                citaRepository,
                citaServicioRepository,
                mascotaRepository,
                usuarioRepository,
                usuarioRolRepository,
                servicioRepository,
                precioServicioTamanoRepository,
                disponibilidadTrabajadorService,
                citaMapper,
                Clock.systemDefaultZone());
    }

    CitaGestionService(
            CitaRepository citaRepository,
            CitaServicioRepository citaServicioRepository,
            MascotaRepository mascotaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioRolRepository usuarioRolRepository,
            ServicioRepository servicioRepository,
            PrecioServicioTamanoRepository precioServicioTamanoRepository,
            DisponibilidadTrabajadorService disponibilidadTrabajadorService,
            CitaMapper citaMapper,
            Clock clock) {
        this.citaRepository = citaRepository;
        this.citaServicioRepository = citaServicioRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.servicioRepository = servicioRepository;
        this.precioServicioTamanoRepository = precioServicioTamanoRepository;
        this.disponibilidadTrabajadorService = disponibilidadTrabajadorService;
        this.citaMapper = citaMapper;
        this.clock = clock;
    }

    @Transactional
    public CitaDetalleResponse crear(CrearCitaRequest request, Long registradoPorId) {
        validarInicioFuturo(request.fechaHoraInicio());
        List<ServicioCitaRequest> serviciosRequest = validarServiciosSinDuplicados(request.servicios());

        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new MascotaNoEncontradaException("Mascota no encontrada."));
        validarMascotaYClienteActivos(mascota);

        Usuario registradoPor = usuarioRepository.findById(registradoPorId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario registrador no encontrado."));
        validarUsuarioActivo(registradoPor, "Usuario registrador inactivo.");

        Map<Long, Servicio> serviciosById = cargarServicios(serviciosRequest);
        Map<Long, PrecioServicioTamano> tarifasById = cargarTarifas(serviciosRequest);
        List<ServicioResuelto> serviciosResueltos = resolverServicios(serviciosRequest, serviciosById, tarifasById);
        TipoCita tipoCita = derivarTipoCita(serviciosResueltos);
        LocalDateTime fin = calcularFin(request.fechaHoraInicio(), serviciosResueltos);
        validarMismoDia(request.fechaHoraInicio(), fin);

        Usuario trabajador = usuarioRepository.findByIdForUpdate(request.trabajadorId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Trabajador no encontrado."));
        validarUsuarioActivo(trabajador, "Trabajador no encontrado o inactivo.");
        validarRolCompatible(trabajador.getId(), tipoCita);

        DisponibilidadTrabajadorResponse disponibilidad = disponibilidadTrabajadorService.consultar(
                trabajador.getId(),
                request.fechaHoraInicio(),
                fin);
        if (!disponibilidad.disponible()) {
            throw new CitaConflictException(normalizarTextoOpcional(disponibilidad.motivo()));
        }

        boolean existeSolapamiento = citaRepository.existsSolapamiento(
                trabajador.getId(),
                EstadoAgendaCita.ocupantes(),
                request.fechaHoraInicio(),
                fin,
                null);
        if (existeSolapamiento) {
            throw new CitaConflictException("La cita se solapa con otra cita activa del trabajador.");
        }

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setTrabajadorAsignado(trabajador);
        cita.setRegistradoPor(registradoPor);
        cita.setTipoCita(tipoCita);
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setFechaHoraInicio(request.fechaHoraInicio());
        cita.setFechaHoraFin(fin);
        cita.setMotivoConsulta(normalizarTextoOpcional(request.motivoConsulta()));
        cita.setObservaciones(normalizarTextoOpcional(request.observaciones()));

        Cita citaGuardada = citaRepository.save(cita);
        List<CitaServicio> citaServicios = crearSnapshotsServicios(citaGuardada, serviciosResueltos);
        List<CitaServicio> citaServiciosGuardados = citaServicioRepository.saveAll(citaServicios);
        return citaMapper.toDetalle(citaGuardada, citaServiciosGuardados);
    }

    @Transactional
    CitaDetalleResponse confirmar(Long citaId) {
        return confirmar(citaId, null, Set.of(NombreRol.ADMINISTRADOR));
    }

    @Transactional
    public CitaDetalleResponse confirmar(Long citaId, Long actorId, Set<NombreRol> roles) {
        Cita cita = obtenerCitaParaActualizar(citaId);
        validarActorPuedeOperarCita(cita, actorId, roles);
        validarTransicion(cita.getEstado(), Set.of(EstadoCita.PENDIENTE));
        cita.setEstado(EstadoCita.CONFIRMADA);
        return citaMapper.toDetalle(cita, cargarServiciosCita(cita.getId()));
    }

    @Transactional
    CitaDetalleResponse reprogramar(Long citaId, ReprogramarCitaRequest request) {
        return reprogramar(citaId, request, null, Set.of(NombreRol.ADMINISTRADOR));
    }

    @Transactional
    public CitaDetalleResponse reprogramar(
            Long citaId,
            ReprogramarCitaRequest request,
            Long actorId,
            Set<NombreRol> roles) {
        Cita cita = obtenerCitaParaActualizar(citaId);
        Set<NombreRol> rolesNormalizados = normalizarRoles(roles);
        validarActorPuedeOperarCita(cita, actorId, rolesNormalizados);
        validarEstadoReprogramable(cita.getEstado());

        List<CitaServicio> citaServicios = cargarServiciosCita(cita.getId());
        LocalDateTime nuevoInicio = request.fechaHoraInicio();
        validarInicioFuturo(nuevoInicio);
        LocalDateTime nuevoFin = calcularFinConDuracionesHistoricas(nuevoInicio, citaServicios);
        validarMismoDia(nuevoInicio, nuevoFin);

        Long trabajadorObjetivoId = esProfesionalSinGestionGlobal(rolesNormalizados)
                ? actorId
                : request.trabajadorId();
        Usuario trabajador = usuarioRepository.findByIdForUpdate(trabajadorObjetivoId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Trabajador no encontrado."));
        validarUsuarioActivo(trabajador, "Trabajador no encontrado o inactivo.");
        validarRolCompatible(trabajador.getId(), cita.getTipoCita());

        DisponibilidadTrabajadorResponse disponibilidad = disponibilidadTrabajadorService.consultar(
                trabajador.getId(),
                nuevoInicio,
                nuevoFin);
        if (!disponibilidad.disponible()) {
            throw new CitaConflictException(normalizarTextoOpcional(disponibilidad.motivo()));
        }

        boolean existeSolapamiento = citaRepository.existsSolapamiento(
                trabajador.getId(),
                EstadoAgendaCita.ocupantes(),
                nuevoInicio,
                nuevoFin,
                cita.getId());
        if (existeSolapamiento) {
            throw new CitaConflictException("La cita se solapa con otra cita activa del trabajador.");
        }

        cita.setTrabajadorAsignado(trabajador);
        cita.setFechaHoraInicio(nuevoInicio);
        cita.setFechaHoraFin(nuevoFin);
        return citaMapper.toDetalle(cita, citaServicios);
    }

    @Transactional
    CitaDetalleResponse cancelar(Long citaId, CancelarCitaRequest request) {
        return cancelar(citaId, request, null, Set.of(NombreRol.ADMINISTRADOR));
    }

    @Transactional
    public CitaDetalleResponse cancelar(
            Long citaId,
            CancelarCitaRequest request,
            Long actorId,
            Set<NombreRol> roles) {
        Cita cita = obtenerCitaParaActualizar(citaId);
        validarActorPuedeOperarCita(cita, actorId, roles);
        validarTransicion(cita.getEstado(), Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA));
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setMotivoCancelacion(normalizarTextoRequerido(request.motivo(), "El motivo de cancelacion es obligatorio."));
        return citaMapper.toDetalle(cita, cargarServiciosCita(cita.getId()));
    }

    @Transactional
    CitaDetalleResponse marcarNoAtendida(Long citaId, MarcarNoAtendidaRequest request) {
        return marcarNoAtendida(citaId, request, null, Set.of(NombreRol.ADMINISTRADOR));
    }

    @Transactional
    public CitaDetalleResponse marcarNoAtendida(
            Long citaId,
            MarcarNoAtendidaRequest request,
            Long actorId,
            Set<NombreRol> roles) {
        Cita cita = obtenerCitaParaActualizar(citaId);
        validarActorPuedeOperarCita(cita, actorId, roles);
        validarTransicion(cita.getEstado(), Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA));
        validarInicioAlcanzado(cita.getFechaHoraInicio(), "La cita solo puede marcarse como no atendida cuando el inicio ya ocurrio.");
        cita.setEstado(EstadoCita.NO_ATENDIDA);
        cita.setMotivoNoAtencion(normalizarTextoRequerido(request.motivo(), "El motivo de no atencion es obligatorio."));
        return citaMapper.toDetalle(cita, cargarServiciosCita(cita.getId()));
    }

    @Transactional
    CitaDetalleResponse marcarAtendida(Long citaId, Long actorId) {
        Set<NombreRol> roles = rolesActivos(actorId);
        return marcarAtendida(citaId, actorId, roles);
    }

    @Transactional
    public CitaDetalleResponse marcarAtendida(Long citaId, Long actorId, Set<NombreRol> roles) {
        Cita cita = obtenerCitaParaActualizar(citaId);
        validarActorPuedeMarcarAtendida(cita, actorId, normalizarRoles(roles));
        throw new CitaConflictException("Las citas deben cerrarse mediante su flujo especifico.");
    }

    @Transactional(readOnly = true)
    DisponibilidadCitaResponse consultarDisponibilidad(
            Long trabajadorId,
            TipoCita tipoCita,
            LocalDateTime inicio,
            LocalDateTime fin) {
        return consultarDisponibilidad(
                trabajadorId,
                tipoCita,
                inicio,
                fin,
                null,
                Set.of(NombreRol.ADMINISTRADOR));
    }

    @Transactional(readOnly = true)
    public DisponibilidadCitaResponse consultarDisponibilidad(
            Long trabajadorId,
            TipoCita tipoCita,
            LocalDateTime inicio,
            LocalDateTime fin,
            Long actorId,
            Set<NombreRol> roles) {
        validarAccesoDisponibilidad(trabajadorId, tipoCita, actorId, roles);
        validarRangoDisponibilidad(inicio, fin);

        Usuario trabajador = usuarioRepository.findById(trabajadorId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Trabajador no encontrado."));
        validarUsuarioActivo(trabajador, "Trabajador no encontrado o inactivo.");
        validarRolCompatible(trabajador.getId(), tipoCita);

        DisponibilidadTrabajadorResponse disponibilidadBase = disponibilidadTrabajadorService.consultar(trabajadorId, inicio, fin);
        if (!disponibilidadBase.disponible()) {
            return citaMapper.toDisponibilidad(
                    trabajadorId,
                    tipoCita,
                    inicio,
                    fin,
                    false,
                    false,
                    false,
                    disponibilidadBase.motivo());
        }

        boolean tieneCitaSolapada = citaRepository.existsSolapamiento(
                trabajadorId,
                EstadoAgendaCita.ocupantes(),
                inicio,
                fin,
                null);
        if (tieneCitaSolapada) {
            return citaMapper.toDisponibilidad(
                    trabajadorId,
                    tipoCita,
                    inicio,
                    fin,
                    true,
                    true,
                    false,
                    "La cita se solapa con otra cita activa del trabajador.");
        }

        return citaMapper.toDisponibilidad(
                trabajadorId,
                tipoCita,
                inicio,
                fin,
                true,
                false,
                true,
                disponibilidadBase.motivo());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CitaResumenResponse> listar(
            CitaListadoFiltroRequest filtro,
            Long usuarioId,
            Set<NombreRol> roles) {
        CitaListadoFiltroRequest filtroNormalizado = normalizarFiltro(filtro);
        validarRangoListado(filtroNormalizado.fechaHoraInicioDesde(), filtroNormalizado.fechaHoraInicioHasta());
        AccesoLecturaCita acceso = resolverAccesoListado(filtroNormalizado, usuarioId, roles);

        Pageable pageable = construirPageable(acceso.filtro().page(), acceso.filtro().size(), acceso.filtro().sort());
        Page<Cita> pagina = citaRepository.findAll(construirSpecification(acceso.filtro()), pageable);
        Map<Long, List<CitaServicio>> serviciosPorCitaId = cargarServiciosPorCitaIds(
                pagina.getContent().stream().map(Cita::getId).toList());

        List<CitaResumenResponse> content = pagina.getContent().stream()
                .map(cita -> citaMapper.toResumen(cita, serviciosPorCitaId.getOrDefault(cita.getId(), List.of())))
                .toList();

        return new PaginaResponse<>(
                content,
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CitaDetalleResponse obtener(Long id, Long usuarioId, Set<NombreRol> roles) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada."));
        validarAccesoDetalle(cita, usuarioId, roles);
        return citaMapper.toDetalle(cita, cargarServiciosPorCitaIds(List.of(id)).getOrDefault(id, List.of()));
    }

    private void validarInicioFuturo(LocalDateTime inicio) {
        if (inicio == null || !inicio.isAfter(ahora())) {
            throw new CitaBadRequestException("La fecha y hora de inicio debe estar en el futuro.");
        }
    }

    private CitaListadoFiltroRequest normalizarFiltro(CitaListadoFiltroRequest filtro) {
        if (filtro == null) {
            return new CitaListadoFiltroRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    DEFAULT_PAGE,
                    DEFAULT_SIZE,
                    DEFAULT_SORT);
        }

        return new CitaListadoFiltroRequest(
                filtro.fechaHoraInicioDesde(),
                filtro.fechaHoraInicioHasta(),
                filtro.estado(),
                filtro.tipoCita(),
                filtro.clienteId(),
                filtro.mascotaId(),
                filtro.trabajadorId(),
                normalizarTextoOpcional(filtro.busqueda()),
                filtro.page() != null ? filtro.page() : DEFAULT_PAGE,
                filtro.size() != null ? filtro.size() : DEFAULT_SIZE,
                filtro.sort() == null || filtro.sort().isEmpty() ? DEFAULT_SORT : filtro.sort());
    }

    private void validarRangoListado(LocalDateTime fechaHoraInicioDesde, LocalDateTime fechaHoraInicioHasta) {
        if (fechaHoraInicioDesde != null
                && fechaHoraInicioHasta != null
                && fechaHoraInicioDesde.isAfter(fechaHoraInicioHasta)) {
            throw new CitaBadRequestException("El rango de fechas para listado es invalido.");
        }
    }

    private AccesoLecturaCita resolverAccesoListado(CitaListadoFiltroRequest filtro, Long usuarioId, Set<NombreRol> roles) {
        Set<NombreRol> rolesNormalizados = roles == null ? Set.of() : roles;
        if (puedeGestionarTodo(rolesNormalizados)) {
            return new AccesoLecturaCita(filtro);
        }

        Set<TipoCita> tiposPermitidos = tiposPermitidosProfesional(rolesNormalizados);
        if (tiposPermitidos.isEmpty() || usuarioId == null) {
            throw new AccessDeniedException("Acceso denegado.");
        }

        TipoCita tipoResuelto = resolverTipoFiltroProfesional(filtro.tipoCita(), tiposPermitidos);
        return new AccesoLecturaCita(new CitaListadoFiltroRequest(
                filtro.fechaHoraInicioDesde(),
                filtro.fechaHoraInicioHasta(),
                filtro.estado(),
                tipoResuelto,
                filtro.clienteId(),
                filtro.mascotaId(),
                usuarioId,
                filtro.busqueda(),
                filtro.page(),
                filtro.size(),
                filtro.sort()));
    }

    private TipoCita resolverTipoFiltroProfesional(TipoCita tipoSolicitado, Set<TipoCita> tiposPermitidos) {
        if (tipoSolicitado == null) {
            return tiposPermitidos.size() == 1 ? tiposPermitidos.iterator().next() : null;
        }
        if (!tiposPermitidos.contains(tipoSolicitado)) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        return tipoSolicitado;
    }

    private boolean puedeGestionarTodo(Set<NombreRol> roles) {
        return roles.contains(NombreRol.ADMINISTRADOR) || roles.contains(NombreRol.RECEPCIONISTA);
    }

    private Set<TipoCita> tiposPermitidosProfesional(Set<NombreRol> roles) {
        Set<TipoCita> tiposPermitidos = new LinkedHashSet<>();
        if (roles.contains(NombreRol.VETERINARIO)) {
            tiposPermitidos.add(TipoCita.MEDICA);
        }
        if (roles.contains(NombreRol.PELUQUERO)) {
            tiposPermitidos.add(TipoCita.PELUQUERIA);
        }
        return tiposPermitidos;
    }

    private Pageable construirPageable(Integer page, Integer size, List<String> sortRules) {
        if (page == null || page < 0) {
            throw new CitaBadRequestException("El numero de pagina es invalido.");
        }
        if (size == null || size < 1 || size > MAX_SIZE) {
            throw new CitaBadRequestException("El tamano de pagina debe estar entre 1 y 100.");
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String sortRule : sortRules) {
            orders.add(parseSortOrder(sortRule));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private Sort.Order parseSortOrder(String sortRule) {
        if (sortRule == null || sortRule.isBlank()) {
            throw new CitaBadRequestException("El parametro sort es invalido.");
        }

        String[] parts = sortRule.split(",", -1);
        if (parts.length < 1 || parts.length > 2) {
            throw new CitaBadRequestException("El parametro sort es invalido.");
        }

        String requestedField = parts[0].trim();
        if (requestedField.isEmpty()) {
            throw new CitaBadRequestException("El parametro sort es invalido.");
        }
        String safeField = SORT_WHITELIST.get(requestedField);
        if (safeField == null) {
            throw new CitaBadRequestException("El parametro sort es invalido.");
        }

        String direction = parts.length == 2 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        if (parts.length == 2 && direction.isEmpty()) {
            throw new CitaBadRequestException("El parametro sort es invalido.");
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new CitaBadRequestException("El parametro sort es invalido.");
        }

        return direction.equals("desc") ? Sort.Order.desc(safeField) : Sort.Order.asc(safeField);
    }

    private Specification<Cita> construirSpecification(CitaListadoFiltroRequest filtro) {
        return Specification.where(CitaSpecifications.fechaHoraInicioDesde(filtro.fechaHoraInicioDesde()))
                .and(CitaSpecifications.fechaHoraInicioHasta(filtro.fechaHoraInicioHasta()))
                .and(CitaSpecifications.estadoEquals(filtro.estado()))
                .and(CitaSpecifications.tipoCitaEquals(filtro.tipoCita()))
                .and(CitaSpecifications.clienteIdEquals(filtro.clienteId()))
                .and(CitaSpecifications.mascotaIdEquals(filtro.mascotaId()))
                .and(CitaSpecifications.trabajadorIdEquals(filtro.trabajadorId()))
                .and(CitaSpecifications.search(filtro.busqueda()));
    }

    private void validarAccesoDetalle(Cita cita, Long usuarioId, Set<NombreRol> roles) {
        Set<NombreRol> rolesNormalizados = roles == null ? Set.of() : roles;
        if (puedeGestionarTodo(rolesNormalizados)) {
            return;
        }

        Set<TipoCita> tiposPermitidos = tiposPermitidosProfesional(rolesNormalizados);
        boolean esProfesionalCompatible = tiposPermitidos.contains(cita.getTipoCita());
        boolean esTrabajadorAsignado = usuarioId != null
                && cita.getTrabajadorAsignado() != null
                && usuarioId.equals(cita.getTrabajadorAsignado().getId());

        if (esProfesionalCompatible && esTrabajadorAsignado) {
            return;
        }

        throw new AccessDeniedException("Acceso denegado.");
    }

    private Map<Long, List<CitaServicio>> cargarServiciosPorCitaIds(List<Long> citaIds) {
        if (citaIds == null || citaIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<CitaServicio>> serviciosPorCitaId = new LinkedHashMap<>();
        for (CitaServicio citaServicio : citaServicioRepository.findByCitaIdIn(citaIds)) {
            Long citaId = citaServicio.getCita().getId();
            serviciosPorCitaId.computeIfAbsent(citaId, ignored -> new ArrayList<>()).add(citaServicio);
        }
        return serviciosPorCitaId;
    }

    private void validarMascotaYClienteActivos(Mascota mascota) {
        if (!Boolean.TRUE.equals(mascota.getActivo())) {
            throw new CitaConflictException("Mascota inactiva.");
        }
        Cliente cliente = mascota.getCliente();
        if (cliente == null || !Boolean.TRUE.equals(cliente.getActivo())) {
            throw new CitaConflictException("El cliente de la mascota esta inactivo.");
        }
    }

    private void validarUsuarioActivo(Usuario usuario, String mensaje) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new CitaConflictException(mensaje);
        }
    }

    private List<ServicioCitaRequest> validarServiciosSinDuplicados(List<ServicioCitaRequest> servicios) {
        if (servicios == null || servicios.isEmpty()) {
            throw new CitaBadRequestException("Debe registrarse al menos un servicio.");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (ServicioCitaRequest servicio : servicios) {
            if (!ids.add(servicio.servicioId())) {
                throw new CitaConflictException("No se permiten servicios duplicados en la cita.");
            }
        }
        return servicios;
    }

    private Map<Long, Servicio> cargarServicios(List<ServicioCitaRequest> serviciosRequest) {
        Set<Long> servicioIds = serviciosRequest.stream()
                .map(ServicioCitaRequest::servicioId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        List<Servicio> servicios = new ArrayList<>();
        servicioRepository.findAllById(servicioIds).forEach(servicios::add);
        if (servicios.size() != servicioIds.size()) {
            throw new ServicioNoEncontradoException("Servicio no encontrado.");
        }

        Map<Long, Servicio> serviciosById = new LinkedHashMap<>();
        for (Servicio servicio : servicios) {
            if (!Boolean.TRUE.equals(servicio.getActivo())) {
                throw new CitaConflictException("Servicio inactivo.");
            }
            serviciosById.put(servicio.getId(), servicio);
        }
        return serviciosById;
    }

    private Map<Long, PrecioServicioTamano> cargarTarifas(List<ServicioCitaRequest> serviciosRequest) {
        Set<Long> tarifaIds = serviciosRequest.stream()
                .map(ServicioCitaRequest::precioServicioTamanoId)
                .filter(id -> id != null)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (tarifaIds.isEmpty()) {
            return Map.of();
        }

        List<PrecioServicioTamano> tarifas = precioServicioTamanoRepository.findByIdInWithServicio(tarifaIds);
        if (tarifas.size() != tarifaIds.size()) {
            throw new ServicioNoEncontradoException("Tarifa no encontrada.");
        }

        Map<Long, PrecioServicioTamano> tarifasById = new LinkedHashMap<>();
        for (PrecioServicioTamano tarifa : tarifas) {
            tarifasById.put(tarifa.getId(), tarifa);
        }
        return tarifasById;
    }

    private List<ServicioResuelto> resolverServicios(
            List<ServicioCitaRequest> serviciosRequest,
            Map<Long, Servicio> serviciosById,
            Map<Long, PrecioServicioTamano> tarifasById) {
        List<ServicioResuelto> servicios = new ArrayList<>();
        for (ServicioCitaRequest servicioRequest : serviciosRequest) {
            Servicio servicio = serviciosById.get(servicioRequest.servicioId());
            PrecioServicioTamano tarifa = null;
            BigDecimal precioAplicado;
            Integer duracionAplicada;

            if (servicioRequest.precioServicioTamanoId() != null) {
                tarifa = tarifasById.get(servicioRequest.precioServicioTamanoId());
                if (tarifa == null
                        || !Boolean.TRUE.equals(tarifa.getActivo())
                        || tarifa.getServicio() == null
                        || !tarifa.getServicio().getId().equals(servicio.getId())) {
                    throw new CitaConflictException("La tarifa indicada no es valida para el servicio.");
                }
                precioAplicado = tarifa.getPrecio();
                duracionAplicada = tarifa.getDuracionMinutos();
            } else {
                if (servicio.getPrecioBase() == null) {
                    throw new CitaConflictException("El servicio no tiene precio base utilizable.");
                }
                precioAplicado = servicio.getPrecioBase();
                duracionAplicada = servicio.getDuracionMinutos();
            }

            servicios.add(new ServicioResuelto(servicio, tarifa, precioAplicado, duracionAplicada));
        }
        return servicios;
    }

    private TipoCita derivarTipoCita(List<ServicioResuelto> servicios) {
        TipoCita tipoCita = null;
        for (ServicioResuelto servicio : servicios) {
            TipoCita tipoActual = mapearTipoCita(servicio.servicio().getTipoServicio());
            if (tipoCita == null) {
                tipoCita = tipoActual;
                continue;
            }
            if (tipoCita != tipoActual) {
                throw new CitaConflictException("No se permite mezclar servicios medicos y de peluqueria.");
            }
        }
        return tipoCita;
    }

    private TipoCita mapearTipoCita(TipoServicio tipoServicio) {
        if (tipoServicio == TipoServicio.MEDICO) {
            return TipoCita.MEDICA;
        }
        return TipoCita.PELUQUERIA;
    }

    private LocalDateTime calcularFin(LocalDateTime inicio, Collection<ServicioResuelto> servicios) {
        int duracionTotal = servicios.stream()
                .map(ServicioResuelto::duracionAplicadaMinutos)
                .reduce(0, Integer::sum);
        return inicio.plusMinutes(duracionTotal);
    }

    private void validarMismoDia(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new CitaConflictException("La cita no puede cruzar de dia.");
        }
    }

    private void validarRolCompatible(Long trabajadorId, TipoCita tipoCita) {
        List<UsuarioRol> roles = usuarioRolRepository.findActivosByUsuarioId(trabajadorId);
        Set<NombreRol> nombresRoles = roles.stream()
                .map(UsuarioRol::getRol)
                .filter(rol -> rol != null && Boolean.TRUE.equals(rol.getActivo()))
                .map(rol -> rol.getNombre())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        NombreRol rolRequerido = tipoCita == TipoCita.MEDICA ? NombreRol.VETERINARIO : NombreRol.PELUQUERO;
        if (!nombresRoles.contains(rolRequerido)) {
            throw new CitaConflictException("El trabajador no tiene un rol compatible con la cita.");
        }
    }

    private void validarTransicion(EstadoCita estadoActual, Set<EstadoCita> estadosPermitidos) {
        if (!estadosPermitidos.contains(estadoActual)) {
            throw new CitaConflictException("La transicion de estado solicitada no es valida para la cita.");
        }
    }

    private void validarEstadoReprogramable(EstadoCita estadoActual) {
        if (!Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA).contains(estadoActual)) {
            throw new CitaConflictException("Solo se pueden reprogramar citas pendientes o confirmadas.");
        }
    }

    private void validarInicioAlcanzado(LocalDateTime fechaHoraInicio, String mensaje) {
        if (fechaHoraInicio == null || fechaHoraInicio.isAfter(ahora())) {
            throw new CitaConflictException(mensaje);
        }
    }

    private void validarRangoDisponibilidad(LocalDateTime inicio, LocalDateTime fin) {
        validarInicioFuturo(inicio);
        if (fin == null || !inicio.isBefore(fin)) {
            throw new CitaBadRequestException("El rango de disponibilidad es invalido.");
        }
        validarMismoDia(inicio, fin);
    }

    private void validarActorPuedeMarcarAtendida(Cita cita, Long actorId, Set<NombreRol> rolesActor) {
        if (rolesActor.contains(NombreRol.ADMINISTRADOR)) {
            return;
        }

        NombreRol rolCompatible = cita.getTipoCita() == TipoCita.MEDICA ? NombreRol.VETERINARIO : NombreRol.PELUQUERO;
        boolean esTrabajadorAsignado = cita.getTrabajadorAsignado() != null
                && cita.getTrabajadorAsignado().getId() != null
                && cita.getTrabajadorAsignado().getId().equals(actorId);
        if (esTrabajadorAsignado && rolesActor.contains(rolCompatible)) {
            return;
        }

        throw new AccessDeniedException("Acceso denegado.");
    }

    private void validarActorPuedeOperarCita(Cita cita, Long actorId, Set<NombreRol> roles) {
        Set<NombreRol> rolesNormalizados = normalizarRoles(roles);
        if (puedeGestionarTodo(rolesNormalizados)) {
            return;
        }

        Set<TipoCita> tiposPermitidos = tiposPermitidosProfesional(rolesNormalizados);
        boolean asignada = actorId != null
                && cita.getTrabajadorAsignado() != null
                && actorId.equals(cita.getTrabajadorAsignado().getId());
        if (asignada && tiposPermitidos.contains(cita.getTipoCita())) {
            return;
        }
        throw new AccessDeniedException("Acceso denegado.");
    }

    private void validarAccesoDisponibilidad(
            Long trabajadorId,
            TipoCita tipoCita,
            Long actorId,
            Set<NombreRol> roles) {
        Set<NombreRol> rolesNormalizados = normalizarRoles(roles);
        if (puedeGestionarTodo(rolesNormalizados)) {
            return;
        }
        if (actorId == null
                || !actorId.equals(trabajadorId)
                || !tiposPermitidosProfesional(rolesNormalizados).contains(tipoCita)) {
            throw new AccessDeniedException("Acceso denegado.");
        }
    }

    private boolean esProfesionalSinGestionGlobal(Set<NombreRol> roles) {
        return !puedeGestionarTodo(roles) && !tiposPermitidosProfesional(roles).isEmpty();
    }

    private Set<NombreRol> normalizarRoles(Set<NombreRol> roles) {
        return roles == null ? Set.of() : roles;
    }

    private Set<NombreRol> rolesActivos(Long usuarioId) {
        return usuarioRolRepository.findActivosByUsuarioId(usuarioId).stream()
                .map(UsuarioRol::getRol)
                .filter(rol -> rol != null && Boolean.TRUE.equals(rol.getActivo()))
                .map(rol -> rol.getNombre())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private Cita obtenerCitaParaActualizar(Long citaId) {
        return citaRepository.findByIdForUpdate(citaId)
                .orElseThrow(() -> new CitaNoEncontradaException("Cita no encontrada."));
    }

    private List<CitaServicio> cargarServiciosCita(Long citaId) {
        return citaServicioRepository.findByCitaIdIn(List.of(citaId));
    }

    private List<CitaServicio> crearSnapshotsServicios(Cita cita, List<ServicioResuelto> servicios) {
        List<CitaServicio> citaServicios = new ArrayList<>();
        for (ServicioResuelto servicioResuelto : servicios) {
            CitaServicio citaServicio = new CitaServicio();
            citaServicio.setCita(cita);
            citaServicio.setServicio(servicioResuelto.servicio());
            citaServicio.setPrecioServicioTamano(servicioResuelto.tarifa());
            citaServicio.setPrecioAplicado(servicioResuelto.precioAplicado());
            citaServicio.setDuracionAplicadaMinutos(servicioResuelto.duracionAplicadaMinutos());
            citaServicios.add(citaServicio);
        }
        return citaServicios;
    }

    private LocalDateTime calcularFinConDuracionesHistoricas(LocalDateTime inicio, Collection<CitaServicio> citaServicios) {
        int duracionTotal = citaServicios.stream()
                .map(CitaServicio::getDuracionAplicadaMinutos)
                .reduce(0, Integer::sum);
        return inicio.plusMinutes(duracionTotal);
    }

    private String normalizarTextoRequerido(String value, String mensaje) {
        String normalizado = normalizarTextoOpcional(value);
        if (normalizado == null) {
            throw new CitaConflictException(mensaje);
        }
        return normalizado;
    }

    private String normalizarTextoOpcional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    private record ServicioResuelto(
            Servicio servicio,
            PrecioServicioTamano tarifa,
            BigDecimal precioAplicado,
            Integer duracionAplicadaMinutos) {
    }

    private record AccesoLecturaCita(CitaListadoFiltroRequest filtro) {
    }
}
