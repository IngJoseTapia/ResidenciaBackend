package com.Tapia.ProyectoResidencia.DTO;

public record UpdateUserRequest(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String email,
        String telefono,
        String genero
) {}
