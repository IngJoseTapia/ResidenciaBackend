package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.Contrato;

public record ApiContratoResponse(
        ApiResponse apiResponse,
        Contrato contrato
) {}
