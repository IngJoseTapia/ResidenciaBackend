package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.NotificacionResponse;
import com.Tapia.ProyectoResidencia.DTO.NotificationStatusRequest;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Service.UsuarioService;
import com.Tapia.ProyectoResidencia.Utils.AuthUtils;
import com.Tapia.ProyectoResidencia.Service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> getMyNotificaciones(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas) {

        String correo = AuthUtils.extractEmailFromAuth(authentication);
        List<NotificacionResponse> notificaciones = notificacionService.getNotificacionesPorCorreo(correo, soloNoLeidas);

        return ResponseEntity.ok(notificaciones);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateNotificationStatus(
            @PathVariable Long id,
            @RequestBody NotificationStatusRequest request,
            Authentication authentication) {

        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario usuario = usuarioService.getUsuarioEntityByCorreo(correo);

        notificacionService.actualizarEstadoNotificacion(id, usuario, request);

        return ResponseEntity.ok(Map.of("message", "Estado de notificación actualizado correctamente"));
    }
}
