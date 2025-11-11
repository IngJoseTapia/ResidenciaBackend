package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;

public record ApiLocalidadResponse(
        ApiResponse response,
        LocalidadResponse localidad
) {}
