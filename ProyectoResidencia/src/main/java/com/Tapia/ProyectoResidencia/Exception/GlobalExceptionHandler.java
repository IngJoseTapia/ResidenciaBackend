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
    public ResponseEntity<ApiResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));

        logger.log(Level.WARNING, "Error de validación en DTO: {0}", mensaje);

        return ResponseEntity.badRequest()
                .body(new ApiResponse(mensaje, HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Maneja errores de validación en entidades (ConstraintViolation en JPA/Hibernate).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        logger.log(Level.WARNING, "Error de validación en entidad: {0}", mensaje);

        return ResponseEntity.badRequest()
                .body(new ApiResponse(mensaje, HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Maneja reglas de negocio incumplidas (validaciones manuales).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.log(Level.WARNING, "Regla de negocio incumplida: {0}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(new ApiResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Maneja credenciales inválidas en login.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException ex) {
        logger.log(Level.WARNING, "Credenciales inválidas: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse("Credenciales inválidas", HttpStatus.UNAUTHORIZED.value()));
    }

    /**
     * Maneja tokens inválidos o expirados.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse> handleSecurity(SecurityException ex) {
        logger.log(Level.WARNING, "Token inválido/expirado: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    /**
     * Maneja accesos denegados por falta de permisos.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex) {
        logger.log(Level.WARNING, "Acceso denegado: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Acceso denegado", HttpStatus.FORBIDDEN.value()));
    }

    /**
     * Maneja entidades no encontradas (ej: usuario inexistente).
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse> handleNotFound(NoSuchElementException ex) {
        logger.log(Level.INFO, "Recurso no encontrado: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(ContratoCreationException.class)
    public ResponseEntity<ApiResponse> handleContratoCreation(ContratoCreationException ex) {
        logger.log(Level.SEVERE, "Error al crear contrato: " + ex.getMessage(), ex);

        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(status)
                .body(new ApiResponse("Error al crear el contrato: " + ex.getMessage(), status.value()));
    }

    /**
     * Maneja cualquier RuntimeException no controlada.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntime(RuntimeException ex) {
        logger.log(Level.SEVERE, "Error en tiempo de ejecución: " + ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse("Error inesperado en el servidor", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    /**
     * Maneja cualquier excepción genérica no contemplada.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneral(Exception ex) {
        logger.log(Level.SEVERE, "Error no controlado: " + ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse("Error interno en el servidor", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @ExceptionHandler(BloqueoException.class)
    public ResponseEntity<ApiResponse> handleBloqueo(BloqueoException ex) {
        logger.log(Level.WARNING, "Bloqueo detectado: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // o 429 si quieres
                .body(new ApiResponse(ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(MissingPasswordException.class)
    public ResponseEntity<ApiResponse> handleMissingPassword(MissingPasswordException ex) {
        logger.log(Level.WARNING, "Usuario sin contraseña: {0}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse> handleUserNotFound(UserNotFoundException ex) {
        logger.log(Level.INFO, "Usuario no encontrado: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse> handleInvalidPassword(InvalidPasswordException ex) {
        logger.log(Level.WARNING, "Contraseña inválida: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ApiResponse> handleWeakPassword(WeakPasswordException ex) {
        logger.log(Level.WARNING, "Contraseña débil: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(PasswordChangeException.class)
    public ResponseEntity<ApiResponse> handlePasswordChange(PasswordChangeException ex) {
        logger.log(Level.SEVERE, "Error al cambiar contraseña: " + ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ApiResponse> handleEmailSend(EmailSendException ex) {
        logger.log(Level.WARNING, "Error enviando correo: {0}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.OK) // ⚠️ No es error crítico para el frontend
                .body(new ApiResponse("Ocurrió un problema al enviar notificación por correo: " + ex.getMessage(),
                        HttpStatus.OK.value()));
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse> handleInvalidOperation(InvalidOperationException ex) {
        logger.log(Level.WARNING, "Operación no permitida: {0}", ex.getMessage());// ⚠️ Cambiado de FORBIDDEN a BAD_REQUEST
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }
}
