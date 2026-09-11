package com.veterinaria.backend.vacuna;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veterinaria.backend.vacuna.controller.VacunaController;
import com.veterinaria.backend.vacuna.service.VacunaAplicadaService;
import com.veterinaria.backend.vacuna.service.VacunaService;

@WebMvcTest(VacunaController.class)
@Import(VacunaControllerTest.MethodSecurityConfiguration.class)
class VacunaControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private VacunaService vacunaService;
    @MockitoBean private VacunaAplicadaService aplicadaService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void receptionistCannotReadOrWriteVacunas() throws Exception {
        mockMvc.perform(get("/api/v1/vacunas").with(jwtFor(2L, "RECEPCIONISTA"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/atenciones-medicas/10/vacunas").with(jwtFor(2L, "RECEPCIONISTA"))
                .contentType("application/json").content("{\"vacunaId\":3}")).andExpect(status().isForbidden());
        verify(vacunaService, never()).listar(any(), any(), any());
    }

    @Test
    void peluqueroWithoutVeterinarianCannotReadCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/vacunas").with(jwtFor(2L, "PELUQUERO"))).andExpect(status().isForbidden());
    }

    @Test
    void applicationUsesOnlyUidFromJwt() throws Exception {
        mockMvc.perform(post("/api/v1/atenciones-medicas/10/vacunas").with(jwtFor(7L, "VETERINARIO"))
                .contentType("application/json").content("{\"vacunaId\":3}")).andExpect(status().isOk());
        verify(aplicadaService).aplicar(eq(10L), eq(7L), any());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(Long uid, String role) {
        return jwt().jwt(jwt -> jwt.claim("uid", uid.toString())).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration { }
}
