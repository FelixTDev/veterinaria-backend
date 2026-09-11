package com.veterinaria.backend.security;

import java.io.IOException;

import com.veterinaria.backend.usuario.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActiveUserFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarioRepository;

    public ActiveUserFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (request.getHeader("Authorization") != null && authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Jwt jwt = jwtAuthentication.getToken();
            Object rawUid = jwt.getClaim("uid");
            Long uid = rawUid instanceof Number number ? number.longValue()
                    : rawUid instanceof String value ? parseUid(value) : null;
            if (uid == null || !usuarioRepository.existsByIdAndActivoTrue(uid)) {
                SecurityContextHolder.clearContext();
                new JsonAuthenticationEntryPoint().commence(request, response,
                        new InvalidBearerTokenException("Usuario autenticado invalido."));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Long parseUid(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
