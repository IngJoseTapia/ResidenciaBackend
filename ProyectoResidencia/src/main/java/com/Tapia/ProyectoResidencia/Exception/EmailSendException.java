package com.Tapia.ProyectoResidencia.Exception;

public class EmailSendException extends Exception {
    public EmailSendException(String message) {
        super(message);
    }
    public EmailSendException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
