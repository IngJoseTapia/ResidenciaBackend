package com.Tapia.ProyectoResidencia.DTO;

import jakarta.validation.constraints.NotNull;

public record VocaliaAssign(
        @NotNull(message = "El ID del usuario es obligatorio")
        Long usuarioId,

        @NotNull(message = "El ID de la vocalía es obligatorio")
        Long vocaliaId
) { }
