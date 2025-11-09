package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiAreResponse {
    private ApiResponse apiResponse;
    private AreResponse are;
}
