package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.UsuarioContrato;

public record ApiUsuarioContratoResponse(
        ApiResponse apiResponse,
        UsuarioContrato usuarioContrato
) {}
