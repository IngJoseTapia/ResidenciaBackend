package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.ChangePasswordRequest;
import com.Tapia.ProyectoResidencia.DTO.UpdateUserRequest;
import com.Tapia.ProyectoResidencia.DTO.UserResponse;
import com.Tapia.ProyectoResidencia.Service.UserService;
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
        String email = extractEmailFromAuth(authentication);
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    // Actualizar datos personales
    @PutMapping("/info")
    public ResponseEntity<String> updateMyInfo(Authentication authentication,
                                               @RequestBody UpdateUserRequest request) {
        String email = extractEmailFromAuth(authentication);
        userService.updateUser(email, request);
        return ResponseEntity.ok("Datos actualizados correctamente");
    }

    // Cambiar contraseña
    @PutMapping("/info/password")
    public ResponseEntity<String> changeMyPassword(Authentication authentication,
                                                   @RequestBody ChangePasswordRequest request) {
        String email = extractEmailFromAuth(authentication);
        userService.changePassword(email, request);
        return ResponseEntity.ok("Contraseña actualizada correctamente");
    }

    // Metodo auxiliar para extraer el email desde el JWT
    private String extractEmailFromAuth(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // JWT normal
            return jwtAuth.getToken().getClaimAsString("email");
        } else if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Auth) {
            // Login con Google u otro proveedor OAuth2
            return oauth2Auth.getPrincipal().getAttribute("email");
        } else if (authentication != null && authentication.getName() != null) {
            // Login tradicional (form login)
            return authentication.getName();
        } else {
            throw new RuntimeException("No se pudo obtener el email del usuario autenticado");
        }
    }
}
