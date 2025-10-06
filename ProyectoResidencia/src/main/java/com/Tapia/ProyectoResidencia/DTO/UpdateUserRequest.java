package com.Tapia.ProyectoResidencia.DTO;

public record UpdateUserRequest(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String telefono,
        String genero
) {}
