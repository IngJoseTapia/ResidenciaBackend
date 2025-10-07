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
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import com.Tapia.ProyectoResidencia.Utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UsuarioRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailLogService emailLogService;
    private final AccountBlockService  accountBlockService;
    private final SystemLogService systemLogService;
    private final IpBlockService ipBlockService;
    private final NotificacionService notificacionService;

    public UserResponse getUserByCorreo(String correo) {
        var user = userRepository.findByCorreo(correo)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        boolean tieneContrasena = user.getContrasena() != null
                && !user.getContrasena().isBlank()
                && !user.getContrasena().startsWith("AUTO_");

        if(!tieneContrasena) {
            if (!notificacionService.existeNotificacionUsuario(user, NotificationTemplate.GENERAR_CONTRASENA)) {
                notificacionService.createNotificationSystem(user, NotificationTemplate.GENERAR_CONTRASENA);
            }
        }

        return new UserResponse(
                user.getNombre(),
                user.getApellidoPaterno(),
                user.getApellidoMaterno(),
                user.getCorreo(),
                user.getTelefono(),
                user.getGenero(),
                tieneContrasena // enviar al frontend
        );
    }

    public Usuario getUsuarioEntityByCorreo(String correo) {
        return userRepository.findByCorreo(correo)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
    }

    public void updateUser(String correo, UpdateUserRequest request, String ip) {
        var user = userRepository.findByCorreo(correo)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        // Validación especial para usuarios sin contraseña (login Google)
        if (user.getContrasena() == null
                || user.getContrasena().isBlank()
                || user.getContrasena().startsWith("AUTO_")) {
            throw new MissingPasswordException(
                    "Debes generar una contraseña primero para poder actualizar tu información personal."
            );
        }

        try {
            // Actualización de datos
            user.setNombre(request.nombre());
            user.setApellidoPaterno(request.apellidoPaterno());
            user.setApellidoMaterno(request.apellidoMaterno());
            user.setGenero(request.genero());
            user.setTelefono(request.telefono());

            userRepository.save(user);
            systemLogService.registrarLogUsuario(
                    user, Evento.UPDATE_INFO_USUARIO_EXITOSO, Resultado.EXITO, Sitio.WEB, ip, null
            );
        } catch (DataIntegrityViolationException e) {
            systemLogService.registrarLogUsuario(
                    user, Evento.UPDATE_INFO_USUARIO_FALLIDO, Resultado.FALLO, Sitio.WEB, ip,
                    "Violación de integridad en la BD: " + e.getMostSpecificCause().getMessage()
            );
            throw new RuntimeException("Error de integridad de datos al actualizar usuario", e);

        } catch (OptimisticLockingFailureException e) {
            systemLogService.registrarLogUsuario(
                    user, Evento.UPDATE_INFO_USUARIO_FALLIDO, Resultado.FALLO, Sitio.WEB, ip,
                    "Conflicto de concurrencia al actualizar usuario"
            );
            throw new RuntimeException("El usuario fue actualizado por otro proceso. Intente de nuevo.", e);

        } catch (Exception e) {
            systemLogService.registrarLogUsuario(
                    user, Evento.UPDATE_INFO_USUARIO_FALLIDO, Resultado.FALLO, Sitio.WEB, ip,
                    "Error inesperado: " + e.getMessage()
            );
            throw new RuntimeException("Error inesperado al actualizar usuario", e);
        }
    }

    public void changePassword(String correo, ChangePasswordRequest request, Sitio sitio, String ip) {
        var user = userRepository.findByCorreo(correo)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

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
                user.setContrasena(passwordEncoder.encode(request.nuevaPassword()));
                userRepository.save(user);
                systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_PRIMERA_VEZ, Resultado.EXITO, sitio, ip, null);
            } else {
                // Guardar nueva contraseña
                user.setContrasena(passwordEncoder.encode(request.nuevaPassword()));
                userRepository.save(user);
                systemLogService.registrarLogUsuario(user, Evento.PASSWORD_CHANGE_EXITOSO, Resultado.EXITO, sitio, ip, null);
            }

            // 🚨 Enviar correo de notificación
            emailLogService.notificarUsuarios(user, Evento.PASSWORD_CHANGE_EXITOSO, Date.from(Instant.now()), null);

            // ✅ Cambio exitoso → limpiar fallos y registrar cambio
            accountBlockService.limpiarBloqueo(user, Evento.PASSWORD_CHANGE_RECHAZADO);
            accountBlockService.registrarCambioPasswordExitoso(user, Evento.PASSWORD_CHANGE_EXITOSO, ip);
            ipBlockService.limpiarIntentos(ip);
            notificacionService.marcarNotificacionResuelta(user, NotificationTemplate.GENERAR_CONTRASENA);
            if(notificacionService.existeNotificacionUsuario(user, NotificationTemplate.GENERAR_CONTRASENA))
                notificacionService.eliminarNotificacionesPorTemplate(user, NotificationTemplate.GENERAR_CONTRASENA);
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
