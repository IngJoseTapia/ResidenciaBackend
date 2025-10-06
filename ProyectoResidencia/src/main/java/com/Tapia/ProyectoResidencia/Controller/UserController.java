package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.ChangePasswordRequest;
import com.Tapia.ProyectoResidencia.DTO.UpdateUserRequest;
import com.Tapia.ProyectoResidencia.DTO.UserResponse;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ErrorResponse;
import com.Tapia.ProyectoResidencia.Security.IpUtils;
import com.Tapia.ProyectoResidencia.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Obtener datos personales del usuario autenticado
    @GetMapping("/info")
    public ResponseEntity<UserResponse> getMyInfo(Authentication authentication) {
        String correo = extractEmailFromAuth(authentication);
        UserResponse user = userService.getUserByCorreo(correo);
        return ResponseEntity.ok(user);
    }

    // Actualizar datos personales
    @PutMapping("/info")
    public ResponseEntity<ErrorResponse> updateMyInfo(Authentication authentication,
                                                      @RequestBody UpdateUserRequest request,
                                                      HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        String correo = extractEmailFromAuth(authentication);
        userService.updateUser(correo, request, ip);

        ErrorResponse response = new ErrorResponse("Información actualizada correctamente ✅", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    // Cambiar contraseña
    @PutMapping("/info/password")
    public ResponseEntity<ErrorResponse> changeMyPassword(Authentication authentication,
                                                          @RequestBody ChangePasswordRequest request,
                                                          HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        String correo = extractEmailFromAuth(authentication);
        userService.changePassword(correo, request, Sitio.WEB, ip);
        return ResponseEntity.ok(new ErrorResponse("Contraseña actualizada correctamente ✅", HttpStatus.OK.value()));
    }

    // Método auxiliar para extraer el correo desde el JWT
    private String extractEmailFromAuth(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // JWT normal
            return jwtAuth.getToken().getClaimAsString("correo");
        } else if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Auth) {
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
