package com.Tapia.ProyectoResidencia.DTO;

public record RegisterRequest(
        String email,
        String password,
        String confirmPassword,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String genero,
        String telefono
) {}
