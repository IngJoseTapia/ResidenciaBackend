package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.NotificationTemplate;
import com.Tapia.ProyectoResidencia.Enum.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long idRelacion, // 🔹 ID de la relación NotificacionUsuario
        String titulo,
        String mensaje,
        TipoNotificacion tipoNotificacion,
        NotificationTemplate template,
        Boolean leida,
        Boolean resuelta,
        LocalDateTime fechaRecepcion, // 🔹 fecha en que se asignó al usuario
        LocalDateTime fechaLectura    // 🔹 fecha en que la leyó (si existe)
) {}
