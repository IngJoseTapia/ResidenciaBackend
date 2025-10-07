package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.ChangePasswordRequest;
import com.Tapia.ProyectoResidencia.DTO.UpdateUserRequest;
import com.Tapia.ProyectoResidencia.DTO.UserResponse;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ErrorResponse;
import com.Tapia.ProyectoResidencia.Utils.AuthUtils;
import com.Tapia.ProyectoResidencia.Utils.IpUtils;
import com.Tapia.ProyectoResidencia.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Obtener datos personales del usuario autenticado
    @GetMapping("/info")
    public ResponseEntity<UserResponse> getMyInfo(Authentication authentication) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        UserResponse user = userService.getUserByCorreo(correo);
        return ResponseEntity.ok(user);
    }

    // Actualizar datos personales
    @PutMapping("/info")
    public ResponseEntity<ErrorResponse> updateMyInfo(Authentication authentication,
                                                      @RequestBody UpdateUserRequest request,
                                                      HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        String correo = AuthUtils.extractEmailFromAuth(authentication);
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
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        userService.changePassword(correo, request, Sitio.WEB, ip);
        return ResponseEntity.ok(new ErrorResponse("Contraseña actualizada correctamente ✅", HttpStatus.OK.value()));
    }
}
