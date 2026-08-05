package com.veterinaria.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("securityAccess")
public class SecurityAccess {

    public boolean isCurrentUser(Long userId, Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }
        Object claim = jwtAuthentication.getToken().getClaims().get("uid");
        return claim != null && userId != null && userId.toString().equals(claim.toString());
    }
}
