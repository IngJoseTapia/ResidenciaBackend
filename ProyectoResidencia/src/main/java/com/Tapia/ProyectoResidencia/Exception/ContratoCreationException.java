package com.Tapia.ProyectoResidencia.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ContratoCreationException extends RuntimeException {

  private final HttpStatus status;

  public ContratoCreationException(String message) {
    super(message);
    this.status = HttpStatus.INTERNAL_SERVER_ERROR; // valor por defecto
  }

  public ContratoCreationException(String message, Throwable cause) {
    super(message, cause);
    this.status = HttpStatus.INTERNAL_SERVER_ERROR; // valor por defecto
  }

  public ContratoCreationException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public ContratoCreationException(String message, HttpStatus status, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

}
