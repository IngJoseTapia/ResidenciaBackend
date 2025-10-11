package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.NotificacionResponse;
import com.Tapia.ProyectoResidencia.Utils.AuthUtils;
import com.Tapia.ProyectoResidencia.Service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> getMyNotificaciones(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas) {

        String correo = AuthUtils.extractEmailFromAuth(authentication);
        List<NotificacionResponse> notificaciones = notificacionService.getNotificacionesPorCorreo(correo, soloNoLeidas);

        return ResponseEntity.ok(notificaciones);
    }
}
