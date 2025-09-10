package com.Tapia.ProyectoResidencia.DTO;

public record UserResponse(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String email,
        String telefono,
        String genero
) {}

