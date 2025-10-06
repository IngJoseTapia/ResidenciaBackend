package com.Tapia.ProyectoResidencia.Exception;

public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) {
        super(message);
    }
}
