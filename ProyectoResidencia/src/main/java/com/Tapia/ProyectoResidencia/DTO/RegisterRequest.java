package com.Tapia.ProyectoResidencia.DTO;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotNull @NotBlank @Email String email,
        @NotNull @NotBlank @Size(min = 8) String password,
        @NotNull @NotBlank @Size(min = 8) String confirmPassword,
        @NotNull @NotBlank @Size(max = 50) String nombre,
        @NotNull @NotBlank @Size(max = 50) String apellidoPaterno,
        @NotNull @NotBlank @Size(max = 50) String apellidoMaterno,
        @NotNull @Pattern(regexp = "^(Masculino|Femenino|Otro)$") String genero,
        @NotNull @Pattern(regexp = "^[0-9]{10}$") String telefono
) {}
