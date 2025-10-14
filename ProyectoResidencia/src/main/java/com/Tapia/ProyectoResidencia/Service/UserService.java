package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.ChangePasswordRequest;
import com.Tapia.ProyectoResidencia.DTO.UpdateUserRequest;
import com.Tapia.ProyectoResidencia.DTO.UserResponse;
import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.NotificationTemplate;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.*;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Utils.PasswordUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final EmailLogService emailLogService;
    private final AccountBlockService  accountBlockService;
    private final SystemLogService systemLogService;
    private final IpBlockService ipBlockService;
    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    public UserResponse getUserByCorreo(String correo) {
        Usuario user = usuarioService.getUsuarioEntityByCorreo(correo);

        boolean tieneContrasena = user.getContrasena() != null && !user.getContrasena().isBlank() && !user.getContrasena().startsWith("AUTO_");
        if(!tieneContrasena) {
            if (!notificacionService.existeNotificacionUsuario(user, NotificationTemplate.GENERAR_CONTRASENA)) {
                notificacionService.createNotificationSystem(user, NotificationTemplate.GENERAR_CONTRASENA);
            }
        }

        boolean perfilIncompleto =
                user.getApellidoPaterno() == null || user.getApellidoMaterno() == null || user.getGenero() == null ||
                        user.getApellidoPaterno().startsWith("N/A") || user.getApellidoMaterno().startsWith("N/A") ||
                        user.getGenero().equalsIgnoreCase("Otro");

        if (perfilIncompleto) {
            if (!notificacionService.existeNotificacionUsuario(user, NotificationTemplate.PERFIL_INCOMPLETO)) {
                try {
                    notificacionService.createNotificationSystem(user, NotificationTemplate.PERFIL_INCOMPLETO);
                } catch (DataIntegrityViolationException e) {
                    // Evita error "Duplicate entry" si ya existe una notificación en BD
                    System.out.println("⚠️ Notificación 'PERFIL_INCOMPLETO' ya existente para el usuario " + user.getCorreo());
                }
            }
        }

        return new UserResponse(
                user.getNombre(),
                user.getApellidoPaterno(),
                user.getApellidoMaterno(),
                user.getCorreo(),
                user.getTelefono(),
                user.getGenero(),
                user.getRol(),
                tieneContrasena // enviar al frontend
        );
    }

    public void updateUser(String correo, UpdateUserRequest request, String ip) {
        Usuario user = usuarioService.getUsuarioEntityByCorreo(correo);

        // Validación especial para usuarios sin contraseña (login Google)
        if (user.getContrasena() == null || user.getContrasena().isBlank() || user.getContrasena().startsWith("AUTO_")) {
            throw new MissingPasswordException("Debes generar una contraseña primero para poder actualizar tu información personal.");
        }

        try {
            Usuario usuario = usuarioService.actualizarUsuario(user, request);
            boolean perfilCompleto =
                    !user.getApellidoPaterno().startsWith("N/A") &&
                            !user.getApellidoMaterno().startsWith("N/A") &&
                            !user.getGenero().matches("Otro");
            if (perfilCompleto && notificacionService.existeNotificacionUsuario(usuario, NotificationTemplate.PERFIL_INCOMPLETO)) {
                notificacionService.resolverYEliminarNotificaciones(usuario, NotificationTemplate.PERFIL_INCOMPLETO);
            }
            systemLogService.registrarLogUsuario(
                    usuario, Evento.UPDATE_INFO_USUARIO_EXITOSO, Resultado.EXITO, Sitio.WEB, ip, null
            );
        } catch (DataIntegrityViolationException e) {
            systemLogService.registrarLogUsuario(
                    user, Evento.UPDATE_INFO_USUARIO_FALLIDO, Resultado.FALLO, Sitio.WEB, ip,
                    "Violación de integridad en la BD: " + e.getMostSpecificCause().getMessage()
            );
            throw e;

        } catch (OptimisticLockingFailureException e) {
            systemLogService.registrarLogUsuario(
                    user, Evento.UPDATE_INFO_USUARIO_FALLIDO, Resultado.FALLO, Sitio.WEB, ip,
                    "Conflicto de concurrencia al actualizar usuario"
            );
            throw e;
        } catch (TransactionSystemException e) {
            // ✅ Extraer ConstraintViolationException si existe
            Throwable t = e.getRootCause();
            if (t instanceof ConstraintViolationException cve) {
                throw cve; // lo dejamos llegar limpio al GlobalExceptionHandler
            }
            throw e; // otro error de transacción
        }
    }

    public void changePassword(String correo, ChangePasswordRequest request, Sitio sitio, String ip) {
        Usuario user = usuarioService.getUsuarioEntityByCorreo(correo);

        if (ipBlockService.estaBloqueada(ip)) {
            systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_FALLIDO, Resultado.FALLO, sitio, ip, "0");
            throw new BloqueoException("La IP está bloqueada. Intente más tarde.");
        }

        // 🚨 Bloqueo por intentos fallidos
        if (accountBlockService.estaBloqueada(user, Evento.PASSWORD_CHANGE_RECHAZADO)) {
            systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_FALLIDO, Resultado.FALLO, sitio, ip, "1");
            throw new InvalidPasswordException("Has excedido los intentos. Intenta más tarde.");
        }

        try {
            boolean esPasswordAuto = user.getContrasena() != null
                    && user.getContrasena().startsWith("AUTO_");

            if (!esPasswordAuto && user.getContrasena() != null && !user.getContrasena().isBlank()) {
                // Caso A: usuario con contraseña real
                if (request.passwordActual() == null || request.passwordActual().isBlank()) {
                    systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_FALLIDO, Resultado.FALLO, sitio, ip, "2");
                    ipBlockService.registrarIntentoFallido(ip);
                    throw new InvalidPasswordException("Debes ingresar tu contraseña actual");
                }
                if (!passwordEncoder.matches(request.passwordActual(), user.getContrasena())) {
                    accountBlockService.registrarIntentoFallido(user, Evento.PASSWORD_CHANGE_RECHAZADO, ip);
                    systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_RECHAZADO, Resultado.FALLO, sitio, ip, null);
                    throw new InvalidPasswordException("Contraseña actual incorrecta");
                }
            } else {
                // Caso B: usuario con contraseña AUTO_ (Google) o sin contraseña → no debe mandar actual
                if (request.passwordActual() != null && !request.passwordActual().isBlank()) {
                    systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_FALLIDO, Resultado.FALLO, sitio, ip, "3");
                    throw new InvalidPasswordException("No debes enviar contraseña actual en tu primer configuración");
                }
            }

            // 🚨 Límite de cambios en 24 horas
            if (accountBlockService.estaBloqueada(user, Evento.PASSWORD_CHANGE_EXITOSO)) {
                systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_EXCESIVOS, Resultado.FALLO, sitio, ip, null);
                throw new InvalidPasswordException("Has alcanzado el límite de cambios de contraseña en 24h. Tu cuenta fue bloqueada temporalmente.");
            }

            // Validaciones comunes
            if (request.nuevaPassword() == null || request.nuevaPassword().isBlank() || request.confirmarPassword() == null || request.confirmarPassword().isBlank()) {
                systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_FALLIDO, Resultado.FALLO, sitio, ip, "4");
                ipBlockService.registrarIntentoFallido(ip);
                throw new InvalidPasswordException("Debes ingresar y confirmar la nueva contraseña");
            }
            if (!request.nuevaPassword().equals(request.confirmarPassword())) {
                systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_FALLIDO, Resultado.FALLO, sitio, ip, "5");
                throw new InvalidPasswordException("Las contraseñas no coinciden");
            }

            // ✅ Validar fuerza de contraseña (mismo criterio que en AuthService)
            if (PasswordUtils.isWeakPassword(request.nuevaPassword())) {
                systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_FALLIDO, Resultado.FALLO, sitio, ip, "6");
                throw new WeakPasswordException("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.");
            }

            if (user.getContrasena().startsWith("AUTO_")){
                // Guardar nueva contraseña por primera vez
                usuarioService.actualizarContrasena(user, request);
                systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_PRIMERA_VEZ, Resultado.EXITO, sitio, ip, null);
            } else {
                // Guardar nueva contraseña
                usuarioService.actualizarContrasena(user, request);
                systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_EXITOSO, Resultado.EXITO, sitio, ip, null);
            }

            // 🚨 Enviar correo de notificación
            emailLogService.notificarUsuarios(user, Evento.PASSWORD_CHANGE_EXITOSO, Date.from(Instant.now()), null);

            // ✅ Cambio exitoso → limpiar fallos y registrar cambio
            accountBlockService.limpiarBloqueo(user, Evento.PASSWORD_CHANGE_RECHAZADO);
            accountBlockService.registrarCambioPasswordExitoso(user, Evento.PASSWORD_CHANGE_EXITOSO, ip);
            ipBlockService.limpiarIntentos(ip);
            notificacionService.resolverYEliminarNotificaciones(user, NotificationTemplate.GENERAR_CONTRASENA);
        } catch (Exception e) {
            if (e instanceof InvalidPasswordException || e instanceof WeakPasswordException) {
                throw e; // deja que siga al handler global
            }
            systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_ERROR, Resultado.FALLO, sitio, ip, "2");
            throw new PasswordChangeException(
                    "Ocurrió un error inesperado al intentar cambiar la contraseña del usuario "
                            + user.getCorreo() + ". Causa: " + e.getMessage(),
                    e
            );
        }
    }
}
