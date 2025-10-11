package com.Tapia.ProyectoResidencia.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VocaliaCreate(
        @NotBlank(message = "La abreviatura es obligatoria")
        @Size(max = 10, message = "La abreviatura no puede exceder 10 caracteres")
        String abreviatura,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 100, message = "El nombre completo no puede exceder 100 caracteres")
        String nombreCompleto
) { }
