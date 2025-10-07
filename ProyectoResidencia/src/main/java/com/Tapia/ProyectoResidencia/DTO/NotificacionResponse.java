package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        String titulo,
        String mensaje,
        TipoNotificacion tipoNotificacion,
        Boolean leida,
        LocalDateTime fechaLectura,
        LocalDateTime fechaCreacion
) {}
