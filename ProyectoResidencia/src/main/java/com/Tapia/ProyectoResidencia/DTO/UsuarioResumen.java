package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Status;

import java.util.Date;

public record UsuarioResumen(
        Long id,
        String correo,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        Rol rol,
        Status status,
        String telefono,
        String genero,
        Date fechaRegistro
) {}
