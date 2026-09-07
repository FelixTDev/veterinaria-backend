package com.veterinaria.backend.atencionmedica;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veterinaria.backend.atencionmedica.controller.AtencionMedicaController;
import com.veterinaria.backend.atencionmedica.service.AtencionMedicaService;

@WebMvcTest(AtencionMedicaController.class)
@Import(AtencionMedicaControllerTest.MethodSecurityConfiguration.class)
class AtencionMedicaControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AtencionMedicaService service;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void receptionistCannotAccessClinicalEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/citas/10/atencion-medica").with(jwtFor(2L, "RECEPCIONISTA")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/citas/10/atencion-medica").with(jwtFor(2L, "RECEPCIONISTA"))
                        .contentType("application/json").content(validJson()))
                .andExpect(status().isForbidden());
        verify(service, never()).obtenerPorCita(any(), any(), any());
    }

    @Test
    void createUsesOnlyUidFromJwt() throws Exception {
        mockMvc.perform(post("/api/v1/citas/10/atencion-medica").with(jwtFor(7L, "VETERINARIO"))
                        .contentType("application/json").content(validJson()))
                .andExpect(status().isOk());
        verify(service).crear(eq(10L), eq(7L), any());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(Long uid, String role) {
        return jwt().jwt(jwt -> jwt.claim("uid", uid.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String validJson() {
        return "{\"diagnostico\":\"ok\",\"tratamiento\":\"ok\",\"receta\":\"ok\"}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration { }
}
