package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.Vocalia;

public record ApiVocaliaResponse(
        ApiResponse apiResponse,
        Vocalia vocalia
) {}
