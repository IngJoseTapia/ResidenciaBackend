package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.Status;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import java.util.Date;

public record UsuarioActivoResponse(
        Long id,
        String correo,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        Rol rol,
        Date fechaRegistro,
        Status status
) {}
