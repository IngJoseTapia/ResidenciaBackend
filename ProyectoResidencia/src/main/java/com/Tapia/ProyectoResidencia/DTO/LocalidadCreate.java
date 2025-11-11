package com.Tapia.ProyectoResidencia.DTO;

import jakarta.validation.constraints.*;

public record LocalidadCreate(

        @NotBlank(message = "El número de localidad es obligatorio")
        @Size(max = 10, message = "El número de localidad no debe superar los 10 caracteres")
        String numeroLocalidad,

        @NotBlank(message = "El nombre de la localidad es obligatorio")
        @Size(max = 100, message = "El nombre de la localidad no debe superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "Debe especificarse el ID del municipio")
        @Pattern(regexp = "^[0-9]{3}$", message = "El ID del municipio debe tener exactamente 3 dígitos")
        String municipioId
) {}
