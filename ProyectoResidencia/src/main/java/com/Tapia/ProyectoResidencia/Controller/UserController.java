package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.ChangePasswordRequest;
import com.Tapia.ProyectoResidencia.DTO.UpdateUserRequest;
import com.Tapia.ProyectoResidencia.DTO.UserResponse;
import com.Tapia.ProyectoResidencia.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public ResponseEntity<UserResponse> getMyInfo(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email"); // extrae el email desde el token
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    // Actualizar datos personales
    @PutMapping("/info")
    public ResponseEntity<String> updateMyInfo(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody UpdateUserRequest request) {
        String email = jwt.getClaimAsString("email");
        userService.updateUser(email, request);
        return ResponseEntity.ok("Datos actualizados correctamente");
    }

    // Cambiar contraseña
    @PutMapping("/info/password")
    public ResponseEntity<String> changeMyPassword(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestBody ChangePasswordRequest request) {
        String email = jwt.getClaimAsString("email");
        userService.changePassword(email, request);
        return ResponseEntity.ok("Contraseña actualizada correctamente");
    }
}
