package com.veterinaria.backend.auth.security;

import java.time.Instant;
import java.util.List;

import com.veterinaria.backend.usuario.entity.Usuario;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder recoveryJwtDecoder;
    private final AuthJwtProperties properties;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Qualifier("recoveryJwtDecoder") JwtDecoder recoveryJwtDecoder,
            AuthJwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.recoveryJwtDecoder = recoveryJwtDecoder;
        this.properties = properties;
    }

    public TokenResult generateAccessToken(Usuario usuario, List<String> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.getExpirationMinutes() * 60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getCorreo().toLowerCase())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("uid", usuario.getId())
                .claim("purpose", TokenPurpose.ACCESS)
                .claim("roles", roles)
                .build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader(), claims)).getTokenValue();
        return new TokenResult(tokenValue, expiresAt.getEpochSecond() - issuedAt.getEpochSecond());
    }

    public TokenResult generateRecoveryToken(Usuario usuario, Long recoveryCodeId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.getRecoveryExpirationMinutes() * 60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getCorreo().toLowerCase())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("uid", usuario.getId())
                .claim("purpose", TokenPurpose.RECOVERY)
                .claim("recoveryCodeId", recoveryCodeId)
                .build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader(), claims)).getTokenValue();
        return new TokenResult(tokenValue, expiresAt.getEpochSecond() - issuedAt.getEpochSecond());
    }

    public Jwt decodeRecoveryToken(String token) {
        return recoveryJwtDecoder.decode(token);
    }

    private JwsHeader jwsHeader() {
        return JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    }

    public record TokenResult(String tokenValue, long expiresIn) {
    }
}
