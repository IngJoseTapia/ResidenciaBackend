package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.RefreshRequest;
import com.Tapia.ProyectoResidencia.DTO.LoginRequest;
import com.Tapia.ProyectoResidencia.DTO.RegisterRequest;
import com.Tapia.ProyectoResidencia.DTO.AuthResponse;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        String mensaje = authService.register(request);
        return ResponseEntity.ok(mensaje);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        // Extraer IP real
        String ip = extractClientIp(httpRequest);

        // Llamar al service con la IP real
        AuthResponse response = authService.login(request, Sitio.WEB, ip);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshRequest request) {
        // Si el refresh token es inválido, se lanza excepción desde el service
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google-success")
    public ResponseEntity<AuthResponse> googleSuccess(Authentication authentication) {
        // Spring te da un principal del tipo OAuth2User
        var oauthUser = (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        // Aquí puedes registrar el usuario en tu BD si no existe
        // y generar un JWT para mantener la sesión
        AuthResponse response = authService.loginWithGoogle(email, name);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/google-failure")
    public ResponseEntity<String> googleFailure() {
        return ResponseEntity.badRequest().body("Error al iniciar sesión con Google");
    }



    private String extractClientIp(HttpServletRequest request) {
        // Revisar cabeceras comunes en caso de que esté detrás de un proxy o load balancer
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // En caso de múltiples IPs en X-Forwarded-For (proxy chain), tomar la primera
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // Convertir IPv6 localhost a IPv4 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}
