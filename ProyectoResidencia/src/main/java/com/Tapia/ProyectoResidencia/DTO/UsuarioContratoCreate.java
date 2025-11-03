package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.StatusContrato;
import jakarta.validation.constraints.*;

public record UsuarioContratoCreate(

        // Usuario activo (nullable si es un usuario eliminado)
        Long usuarioId,

        // Contrato a asignar (obligatorio)
        @NotNull(message = "El ID del contrato es obligatorio")
        Long contratoId,

        // Número de contrato sensible
        @NotBlank(message = "El número de contrato es obligatorio")
        @Size(min = 30, max = 50, message = "El número de contrato debe tener entre 30 y 50 caracteres")
        String numeroContrato,

        // Estado del contrato
        @NotNull(message = "El estado del contrato es obligatorio")
        StatusContrato status,

        // Observaciones opcionales
        String observaciones

) {}
