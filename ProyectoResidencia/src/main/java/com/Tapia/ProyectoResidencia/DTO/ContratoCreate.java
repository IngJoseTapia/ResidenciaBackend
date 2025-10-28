package com.Tapia.ProyectoResidencia.DTO;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoCreate(
        @NotBlank(message = "El puesto es obligatorio")
        String puesto,

        @NotBlank(message = "El código es obligatorio")
        String codigo,

        @NotBlank(message = "El nivel tabular es obligatorio")
        String nivelTabular,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de conclusión es obligatoria")
        LocalDate fechaConclusion,

        @NotBlank(message = "Las actividades genéricas son obligatorias")
        String actividadesGenericas,

        @NotNull(message = "El sueldo es obligatorio")
        @DecimalMin(value = "0.01", message = "El sueldo debe ser mayor que 0")
        @Digits(integer = 6, fraction = 2, message = "El sueldo debe tener máximo 6 dígitos enteros y 2 decimales")
        BigDecimal sueldo
) {}
