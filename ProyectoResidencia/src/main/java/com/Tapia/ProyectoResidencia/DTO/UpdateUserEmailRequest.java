package com.Tapia.ProyectoResidencia.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserEmailRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser válido")
        String nuevoCorreo
) {}
