package com.veterinaria.backend.cita;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veterinaria.backend.cita.controller.CitaController;
import com.veterinaria.backend.cita.dto.CitaListadoFiltroRequest;
import com.veterinaria.backend.cita.dto.CrearCitaRequest;
import com.veterinaria.backend.cita.dto.ReprogramarCitaRequest;
import com.veterinaria.backend.cita.service.CitaGestionService;
import com.veterinaria.backend.usuario.enums.NombreRol;

@WebMvcTest(CitaController.class)
@Import(CitaControllerTest.MethodSecurityTestConfiguration.class)
class CitaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CitaGestionService citaGestionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/citas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidProfessionalCreatingAppointments() throws Exception {
        mockMvc.perform(post("/api/v1/citas")
                        .with(jwtFor(20L, "VETERINARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearJson()))
                .andExpect(status().isForbidden());

        verify(citaGestionService, never()).crear(any(), any());
    }

    @Test
    void shouldCreateUsingOnlyUidFromJwt() throws Exception {
        mockMvc.perform(post("/api/v1/citas")
                        .with(jwtFor(12L, "RECEPCIONISTA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearJson()))
                .andExpect(status().isCreated());

        verify(citaGestionService).crear(any(CrearCitaRequest.class), eq(12L));
    }

    @Test
    void shouldReturnUnauthorizedForOperationalTokenWithoutUid() throws Exception {
        mockMvc.perform(get("/api/v1/citas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPassAuthenticatedProfessionalScopeToListing() throws Exception {
        mockMvc.perform(get("/api/v1/citas")
                        .param("trabajadorId", "999")
                        .param("sort", "fechaHoraInicio,desc")
                        .with(jwtFor(20L, "VETERINARIO")))
                .andExpect(status().isOk());

        verify(citaGestionService).listar(
                any(CitaListadoFiltroRequest.class),
                eq(20L),
                eq(Set.of(NombreRol.VETERINARIO)));
    }

    @Test
    void shouldForbidReceptionistMarkingAttendedBeforeServiceInvocation() throws Exception {
        mockMvc.perform(patch("/api/v1/citas/5/atendida")
                        .with(jwtFor(2L, "RECEPCIONISTA")))
                .andExpect(status().isForbidden());

        verify(citaGestionService, never()).marcarAtendida(any(), any(), any());
    }

    @Test
    void shouldExposeAllOperationalPatchRoutes() throws Exception {
        var admin = jwtFor(1L, "ADMINISTRADOR");
        mockMvc.perform(patch("/api/v1/citas/5/confirmar").with(admin))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/citas/5/reprogramar")
                        .with(jwtFor(1L, "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trabajadorId\":20,\"fechaHoraInicio\":\"2030-01-02T10:00:00\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/citas/5/cancelar")
                        .with(jwtFor(1L, "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Solicitud del cliente\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/citas/5/no-atendida")
                        .with(jwtFor(1L, "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"No se presento\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/citas/5/atendida")
                        .with(jwtFor(1L, "ADMINISTRADOR")))
                .andExpect(status().isOk());

        verify(citaGestionService).reprogramar(eq(5L), any(ReprogramarCitaRequest.class), eq(1L), any());
    }

    @Test
    void shouldExposeDetailAndIsoAvailabilityRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/citas/5").with(jwtFor(20L, "VETERINARIO")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/citas/disponibilidad")
                        .param("trabajadorId", "20")
                        .param("tipoCita", "MEDICA")
                        .param("inicio", "2030-01-02T10:00:00")
                        .param("fin", "2030-01-02T10:30:00")
                        .with(jwtFor(20L, "VETERINARIO")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestForInvalidQueryParameterFormats() throws Exception {
        mockMvc.perform(get("/api/v1/citas/disponibilidad")
                        .param("trabajadorId", "20")
                        .param("tipoCita", "MEDICA")
                        .param("inicio", "fecha-invalida")
                        .param("fin", "2030-01-02T10:30:00")
                        .with(jwtFor(1L, "ADMINISTRADOR")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/citas")
                        .param("estado", "ESTADO_INEXISTENTE")
                        .with(jwtFor(1L, "ADMINISTRADOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedForNonNumericUid() throws Exception {
        mockMvc.perform(get("/api/v1/citas")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("uid", "no-numerico"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(Long uid, String role) {
        return jwt()
                .jwt(jwt -> jwt.claim("uid", uid.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String crearJson() {
        return """
                {
                  "mascotaId": 10,
                  "trabajadorId": 20,
                  "fechaHoraInicio": "2030-01-02T10:00:00",
                  "servicios": [{"servicioId": 30}],
                  "motivoConsulta": "Control"
                }
                """;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {
    }
}
