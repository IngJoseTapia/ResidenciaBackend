package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.Municipio;

public record ApiMunicipioResponse(
        ApiResponse apiResponse,
        Municipio municipio
) { }
