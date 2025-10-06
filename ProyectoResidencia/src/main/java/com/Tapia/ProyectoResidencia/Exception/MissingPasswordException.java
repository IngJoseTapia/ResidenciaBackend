package com.Tapia.ProyectoResidencia.Exception;

public class MissingPasswordException extends RuntimeException {
    public MissingPasswordException(String message) {
        super(message);
    }
}
