package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.*;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Security.IpUtils;
import com.Tapia.ProyectoResidencia.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request,
                                                HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        String mensaje = authService.register(request, ip);
        return ResponseEntity.ok(new ApiResponse("success", mensaje));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        AuthResponse response = authService.login(request, Sitio.WEB, ip);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        // Si el refresh token es inválido, se lanza excepción desde el service
        String ip = IpUtils.extractClientIp(httpRequest);
        AuthResponse response = authService.refreshToken(request.refreshToken(), Sitio.WEB, ip);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google-failure")
    public ResponseEntity<ApiResponse> googleFailure() {
        return ResponseEntity.badRequest().body(new ApiResponse("failure", "Error al iniciar sesión con Google"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        authService.requestPasswordReset(body.get("email"), Sitio.WEB, ip);

        // Respuesta siempre genérica, sin filtrar si el correo existe
        return ResponseEntity.ok(new ApiResponse(
                "success",
                "Si el correo ingresado está registrado, se enviará un código de recuperación."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);

        boolean exito = authService.resetPassword(body.get("token"), body.get("newPassword"), Sitio.WEB, ip);

        if (exito) {
            return ResponseEntity.ok(new ApiResponse("success", "Contraseña actualizada correctamente."));
        } else {
            return ResponseEntity.status(400)
                    .body(new ApiResponse("failure", "No se pudo actualizar la contraseña. Intente nuevamente."));
        }
    }
}
