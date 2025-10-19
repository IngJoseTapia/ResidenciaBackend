package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.Status;

import java.util.Date;

public record UsuarioPendienteAsignacion(
        Long id,
        String correo,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        Date fechaRegistro,
        Status status
) {}
