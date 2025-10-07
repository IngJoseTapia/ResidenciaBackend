package com.Tapia.ProyectoResidencia.Security;

import com.Tapia.ProyectoResidencia.DTO.AuthResponse;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.BloqueoException;
import com.Tapia.ProyectoResidencia.Service.AuthService;
import com.Tapia.ProyectoResidencia.Utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    public CustomOAuth2SuccessHandler(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        var oauthUser = (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        // Reutilizar la clase IpUtils
        String ip = IpUtils.extractClientIp(request);

        try {
            // Si la IP o la cuenta están bloqueadas, AuthService lanzará BloqueoException
            AuthResponse jwtResponse = authService.loginWithGoogle(email, name, Sitio.WEB, ip);

            // ✅ Redirigir con token si todo salió bien
            String redirectUrl = "http://localhost:5173/login/oauth2/code/google"
                    + "?token=" + jwtResponse.token()
                    + "&refreshToken=" + jwtResponse.refreshToken();
            response.sendRedirect(redirectUrl);

        } catch (BloqueoException ex) {
            // 🔴 Redirigir con mensaje de error en caso de bloqueo
            String redirectUrl = "http://localhost:5173/login/oauth2/code/google"
                    + "?error=" + URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        }
    }
}
