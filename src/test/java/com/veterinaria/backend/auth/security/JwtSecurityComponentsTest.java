package com.veterinaria.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class JwtSecurityComponentsTest {

    private final SecretKey secretKey =
            new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    @Test
    void shouldMapRolesToRoleAuthorities() {
        SecurityConfigurationFactory factory = new SecurityConfigurationFactory();
        JwtAuthenticationConverter converter = factory.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ana@test.dev")
                .claim("roles", List.of("ADMINISTRADOR", "RECEPCIONISTA"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_ADMINISTRADOR", "ROLE_RECEPCIONISTA");
    }

    @Test
    void shouldDifferentiateAccessTokenAndRecoveryTokenByPurpose() {
        JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
        JwtDecoder accessDecoder = decoder(TokenPurpose.ACCESS);
        JwtDecoder recoveryDecoder = decoder(TokenPurpose.RECOVERY);

        String accessToken = tokenWithPurpose(encoder, TokenPurpose.ACCESS);
        String recoveryToken = tokenWithPurpose(encoder, TokenPurpose.RECOVERY);

        assertThat(accessDecoder.decode(accessToken).getClaimAsString("purpose")).isEqualTo(TokenPurpose.ACCESS);
        assertThat(recoveryDecoder.decode(recoveryToken).getClaimAsString("purpose")).isEqualTo(TokenPurpose.RECOVERY);
        assertThatThrownBy(() -> accessDecoder.decode(recoveryToken)).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> recoveryDecoder.decode(accessToken)).isInstanceOf(Exception.class);
    }

    private JwtDecoder decoder(String expectedPurpose) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                token -> expectedPurpose.equals(token.getClaimAsString("purpose"))
                        ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                        : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                                new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"))));
        return decoder;
    }

    private String tokenWithPurpose(JwtEncoder encoder, String purpose) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("ana@test.dev")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("uid", 1L)
                .claim("purpose", purpose)
                .claim("roles", List.of("RECEPCIONISTA"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static final class SecurityConfigurationFactory
            extends com.veterinaria.backend.security.SecurityConfiguration {
    }
}
