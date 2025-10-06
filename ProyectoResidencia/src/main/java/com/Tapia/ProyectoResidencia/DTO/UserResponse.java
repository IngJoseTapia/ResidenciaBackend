package com.Tapia.ProyectoResidencia.DTO;

public record UserResponse(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String correo,
        String telefono,
        String genero,
        boolean tieneContrasena // nuevo campo
) {}
