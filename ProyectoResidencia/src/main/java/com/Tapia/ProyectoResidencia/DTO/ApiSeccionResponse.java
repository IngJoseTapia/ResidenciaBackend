package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;

public record ApiSeccionResponse(
        ApiResponse response,
        SeccionResponse seccion
) {}
