package com.Tapia.ProyectoResidencia.Exception;

import lombok.Data;

import java.time.Instant;

@Data
public class ErrorResponse {
    private String mensaje;
    private int codigo;
    private Instant timestamp;

    public ErrorResponse(String mensaje, int codigo) {
        this.mensaje = mensaje;
        this.codigo = codigo;
        this.timestamp = Instant.now();
    }
}
