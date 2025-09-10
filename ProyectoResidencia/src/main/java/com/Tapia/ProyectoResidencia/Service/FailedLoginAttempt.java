package com.Tapia.ProyectoResidencia.Service;

import lombok.Data;

import java.util.Date;

@Data
public class FailedLoginAttempt {
    private int intentos;
    private Date bloqueadaHasta;

    public FailedLoginAttempt() {
        this.intentos = 0;
        this.bloqueadaHasta = null;
    }
}
