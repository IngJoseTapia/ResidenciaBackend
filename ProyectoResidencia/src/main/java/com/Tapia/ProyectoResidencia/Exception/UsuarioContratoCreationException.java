package com.Tapia.ProyectoResidencia.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UsuarioContratoCreationException extends RuntimeException {
  private final HttpStatus status;

  public UsuarioContratoCreationException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }
}
