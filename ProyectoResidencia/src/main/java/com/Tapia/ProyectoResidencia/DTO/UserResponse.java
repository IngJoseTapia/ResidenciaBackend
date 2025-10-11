package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.Rol;

public record UserResponse(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String correo,
        String telefono,
        String genero,
        Rol rol,
        boolean tieneContrasena // nuevo campo
) {}
