package com.Tapia.ProyectoResidencia.Exception;

public class PasswordChangeException extends RuntimeException {
    public PasswordChangeException(String message) {
        super(message);
    }
    public PasswordChangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
