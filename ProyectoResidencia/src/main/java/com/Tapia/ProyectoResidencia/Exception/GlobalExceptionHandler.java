package com.Tapia.ProyectoResidencia.Exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    /**
     * Maneja errores de validación en DTOs (@Valid en RequestBody).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));

        logger.log(Level.WARNING, "Error de validación en DTO: {0}", mensaje);

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(mensaje, HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Maneja errores de validación en entidades (ConstraintViolation en JPA/Hibernate).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        logger.log(Level.WARNING, "Error de validación en entidad: {0}", mensaje);

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(mensaje, HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Maneja reglas de negocio incumplidas (validaciones manuales).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.log(Level.WARNING, "Regla de negocio incumplida: {0}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Maneja credenciales inválidas en login.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        logger.log(Level.WARNING, "Credenciales inválidas: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Credenciales inválidas", HttpStatus.UNAUTHORIZED.value()));
    }

    /**
     * Maneja tokens inválidos o expirados.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
        logger.log(Level.WARNING, "Token inválido/expirado: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    /**
     * Maneja accesos denegados por falta de permisos.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        logger.log(Level.WARNING, "Acceso denegado: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Acceso denegado", HttpStatus.FORBIDDEN.value()));
    }

    /**
     * Maneja entidades no encontradas (ej: usuario inexistente).
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        logger.log(Level.INFO, "Recurso no encontrado: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    /**
     * Maneja cualquier RuntimeException no controlada.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        logger.log(Level.SEVERE, "Error en tiempo de ejecución: " + ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error inesperado en el servidor", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    /**
     * Maneja cualquier excepción genérica no contemplada.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        logger.log(Level.SEVERE, "Error no controlado: " + ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error interno en el servidor", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @ExceptionHandler(BloqueoException.class)
    public ResponseEntity<ErrorResponse> handleBloqueo(BloqueoException ex) {
        logger.log(Level.WARNING, "Bloqueo detectado: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // o 429 si quieres
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(MissingPasswordException.class)
    public ResponseEntity<ErrorResponse> handleMissingPassword(MissingPasswordException ex) {
        logger.log(Level.WARNING, "Usuario sin contraseña: {0}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        logger.log(Level.INFO, "Usuario no encontrado: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        logger.log(Level.WARNING, "Contraseña inválida: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ErrorResponse> handleWeakPassword(WeakPasswordException ex) {
        logger.log(Level.WARNING, "Contraseña débil: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(PasswordChangeException.class)
    public ResponseEntity<ErrorResponse> handlePasswordChange(PasswordChangeException ex) {
        logger.log(Level.SEVERE, "Error al cambiar contraseña: " + ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ErrorResponse> handleEmailSend(EmailSendException ex) {
        logger.log(Level.WARNING, "Error enviando correo: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.OK) // ⚠️ No es error crítico para el frontend
                .body(new ErrorResponse("Ocurrió un problema al enviar notificación por correo: " + ex.getMessage(),
                        HttpStatus.OK.value()));
    }
}
