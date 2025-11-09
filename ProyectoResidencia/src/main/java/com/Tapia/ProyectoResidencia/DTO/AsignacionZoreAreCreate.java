package com.Tapia.ProyectoResidencia.DTO;

import jakarta.validation.constraints.*;

public record AsignacionZoreAreCreate(

        @NotBlank(message = "El año es obligatorio")
        @Pattern(
                regexp = "^(19|20)\\d{2}$",
                message = "El año debe tener exactamente 4 dígitos y estar entre 1990 y 2040"
        )
        String anio,

        @NotNull(message = "Debe seleccionar una ZORE")
        Long zoreId,

        @NotNull(message = "Debe seleccionar una ARE")
        Long areId
) {}
