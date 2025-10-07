package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.NotificacionResponse;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Utils.AuthUtils;
import com.Tapia.ProyectoResidencia.Service.NotificacionService;
import com.Tapia.ProyectoResidencia.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
//@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UserService userService; // para obtener usuario por correo

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> getMyNotificaciones(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas
    ) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario usuario = userService.getUsuarioEntityByCorreo(correo); // obtenemos la entidad
        List<NotificacionResponse> notificaciones = notificacionService
                .getNotificacionesUsuario(usuario, soloNoLeidas)
                .stream()
                .map(nu -> new NotificacionResponse(
                        nu.getNotificacion().getTitulo(),
                        nu.getNotificacion().getMensaje(),
                        nu.getNotificacion().getTipo(),
                        nu.isLeida(),
                        nu.getFechaLectura(),  // fechaLectura
                        nu.getNotificacion().getFechaCreacion() // fechaCreacion
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(notificaciones);
    }
}
