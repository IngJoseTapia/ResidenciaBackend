package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;

public record ApiAsignacionZoreAreResponse(
        ApiResponse apiResponse,
        AsignacionZoreAreResponse asignacionZoreAreResponse
) {}
