package com.veterinaria.backend.peluqueria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

import com.veterinaria.backend.peluqueria.controller.AtencionPeluqueriaController;
import com.veterinaria.backend.peluqueria.service.AtencionPeluqueriaService;

@WebMvcTest(AtencionPeluqueriaController.class)
@Import(AtencionPeluqueriaControllerTest.MethodSecurityConfiguration.class)
class AtencionPeluqueriaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AtencionPeluqueriaService service;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void receptionistCannotAccessHairdresserEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/citas/10/atencion-peluqueria").with(jwtFor(2L, "RECEPCIONISTA")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/citas/10/atencion-peluqueria").with(jwtFor(2L, "RECEPCIONISTA"))
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verify(service, never()).obtenerPorCita(any(), any(), any());
    }

    @Test
    void peluqueroUsesJwtUidAndMultipartPayload() throws Exception {
        mockMvc.perform(multipart("/api/v1/atenciones-peluqueria/10/evidencias")
                        .with(jwtFor(7L, "PELUQUERO"))
                        .param("tipoFoto", "ANTES")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "archivo", "foto.jpg", "image/jpeg", new byte[] {1})))
                .andExpect(status().isCreated());
        verify(service).agregarEvidencia(eq(10L), eq(7L), any(), any(), eq(com.veterinaria.backend.peluqueria.enums.TipoFoto.ANTES));
    }

    @Test
    void veterinarianCannotAccessHairdresserEndpoints() throws Exception {
        mockMvc.perform(patch("/api/v1/atenciones-peluqueria/10/cerrar").with(jwtFor(7L, "VETERINARIO")))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(Long uid, String role) {
        return jwt().jwt(jwt -> jwt.claim("uid", uid.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration { }
}
