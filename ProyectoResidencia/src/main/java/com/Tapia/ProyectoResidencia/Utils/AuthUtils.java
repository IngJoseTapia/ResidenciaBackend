package com.Tapia.ProyectoResidencia.Utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public class AuthUtils {

    private AuthUtils() {
        // Evita instanciación
        throw new IllegalStateException("Utility class");
    }

    public static String extractEmailFromAuth(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // JWT normal
            return jwtAuth.getToken().getClaimAsString("correo");
        } else if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            // Login con Google u otro proveedor OAuth2
            return oauth2Auth.getPrincipal().getAttribute("correo");
        } else if (authentication != null && authentication.getName() != null) {
            // Login tradicional (form login)
            return authentication.getName();
        } else {
            throw new RuntimeException("No se pudo obtener el correo del usuario autenticado");
        }
    }
}
