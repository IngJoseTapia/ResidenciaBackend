package com.Tapia.ProyectoResidencia.Security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomOAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException {

        String errorMessage = URLEncoder.encode(
                exception.getMessage() != null ? exception.getMessage() : "Error desconocido",
                StandardCharsets.UTF_8
        );

        // 🔹 Redirigir al frontend con query param
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:5173/login?error=" + errorMessage);
    }
}
