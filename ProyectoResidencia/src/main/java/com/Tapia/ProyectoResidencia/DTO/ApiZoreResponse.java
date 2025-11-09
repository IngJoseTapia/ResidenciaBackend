package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;

public record ApiZoreResponse(
        ApiResponse response,
        ZoreResponse zore
) {}
