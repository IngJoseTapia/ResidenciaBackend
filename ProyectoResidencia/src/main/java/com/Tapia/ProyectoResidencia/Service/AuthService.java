package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.LoginRequest;
import com.Tapia.ProyectoResidencia.DTO.RegisterRequest;
import com.Tapia.ProyectoResidencia.DTO.AuthResponse;
import com.Tapia.ProyectoResidencia.Enum.*;
import com.Tapia.ProyectoResidencia.Exception.BloqueoException;
import com.Tapia.ProyectoResidencia.Model.PasswordResetToken;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Utils.JwtUtils;
import com.Tapia.ProyectoResidencia.Utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmailLogService emailLogService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AccountBlockService accountBlockService;
    private final IpBlockService ipBlockService;
    private final LoginLogService loginLogService;
    private final UsuarioService usuarioService;
    private final TokenService tokenService;
    private final NotificacionService notificacionService;

    public String register(RegisterRequest request, String ip) {
        // 1. Validaciones de negocio mínimas
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        if (usuarioService.existeUsuarioByCorreo(request.email())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        //2. Mapear y registrar usuario en la base de datos
        Usuario usuario = usuarioService.registrarUsuario(request.email(),request.password(),request.nombre(),request.apellidoPaterno(),request.apellidoMaterno(),request.genero(),request.telefono());

        // Registrar log de registro (opcional)
        loginLogService.registrarLogsUsuario(usuario, Evento.USER_REGISTRADO, Resultado.EXITO, Sitio.WEB, ip, "1");

        // Notificación
        emailLogService.notificarUsuarios(usuario, Evento.USER_REGISTRADO, Date.from(Instant.now()), null);

        return "Usuario registrado correctamente. Espera la aprobación de un administrador.";
    }

    public AuthResponse login(LoginRequest request, Sitio sitio, String ip) {
        Usuario usuario = null;

        try {
            // 0. Revisar bloqueo por IP
            if (ipBlockService.estaBloqueada(ip)) {
                loginLogService.registrarLogsCorreo(request.email(), Evento.LOGIN_FALLIDO, Resultado.FALLO, sitio, ip, "0");
                throw new BloqueoException("La IP está bloqueada. Intente más tarde.");
            }

            // 1. Verificar si el correo existe en la BD
            Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioByCorreo(request.email());
            if (usuarioOpt.isEmpty()) {
                // incrementar contador por IP
                ipBlockService.registrarIntentoFallido(ip);
                loginLogService.registrarLogsCorreo(request.email(), Evento.LOGIN_FALLIDO, Resultado.FALLO, sitio, ip, "1");
                throw new NoSuchElementException("El correo no está registrado");
            }

            usuario = usuarioOpt.get();

            // 2. Revisar bloqueo por cuenta
            if (accountBlockService.estaBloqueada(usuario, Evento.LOGIN_FALLIDO)) {
                loginLogService.registrarLogsUsuario(usuario, Evento.LOGIN_FALLIDO, Resultado.FALLO, sitio, ip, "2");
                throw new BloqueoException("La cuenta está bloqueada. Intente más tarde.");
            } else {
                // limpia si ya expiró
                accountBlockService.limpiarSiExpirado(usuario, Evento.LOGIN_FALLIDO);
            }

            // 3. Autenticar (puede lanzar BadCredentialsException)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // 4. Login exitoso: resetear contadores y generar tokens
            accountBlockService.limpiarBloqueo(usuario, Evento.LOGIN_FALLIDO);
            ipBlockService.limpiarIntentos(ip);

            String jwt = jwtUtils.generateToken(usuario);
            String refreshToken = jwtUtils.generateRefreshToken(usuario);

            // Registrar log de éxito
            loginLogService.registrarLogsUsuario(usuario, Evento.LOGIN_EXITOSO, Resultado.EXITO, sitio, ip, null);

            return new AuthResponse(jwt, refreshToken, usuario.getRol());
        } catch (BadCredentialsException e) {
            //  Contraseña incorrecta
            if (usuario != null) {
                accountBlockService.registrarIntentoFallido(usuario, Evento.LOGIN_FALLIDO, ip); // incrementa y bloquea si aplica
            }
            ipBlockService.registrarIntentoFallido(ip);
            loginLogService.registrarLogsUsuario(usuario, Evento.LOGIN_FALLIDO, Resultado.FALLO, Sitio.WEB, ip, "3");

            throw new BadCredentialsException("Contraseña incorrecta");
        }
    }

    public AuthResponse loginWithGoogle(String correo, String nombre, Sitio sitio, String ip) {
        // 0. Revisar bloqueo por IP
        if (ipBlockService.estaBloqueada(ip)) {
            loginLogService.registrarLogsCorreo(correo, Evento.LOGIN_FALLIDO, Resultado.FALLO, sitio, ip, "0");
            throw new BloqueoException("La IP está bloqueada. Intente más tarde.");
        }

        // 1. Validar parámetros
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo proporcionado por Google no es válido");
        }

        if(!usuarioService.existeUsuarioByCorreo(correo)) {
            Usuario usuario = usuarioService.registrarUsuarioGoogle(correo, nombre);

            emailLogService.notificarUsuarios(usuario, Evento.USER_REGISTRADO_GOOGLE, Date.from(Instant.now()), null);
            loginLogService.registrarLogsUsuario(usuario, Evento.USER_REGISTRADO_GOOGLE, Resultado.EXITO, sitio, ip, null);
            notificacionService.createNotificationSystem(usuario, NotificationTemplate.GENERAR_CONTRASENA);
        }

        Usuario usuario = usuarioService.getUsuarioEntityByCorreo(correo);

        // 3. Revisar bloqueo por cuenta
        if (accountBlockService.estaBloqueada(usuario, Evento.LOGIN_FALLIDO)) {
             loginLogService.registrarLogsUsuario(usuario, Evento.LOGIN_FALLIDO, Resultado.FALLO, sitio, ip, "2");
            throw new BloqueoException("La cuenta está bloqueada. Intente más tarde.");
        } else {
            // limpia si ya expiró
            accountBlockService.limpiarSiExpirado(usuario, Evento.LOGIN_FALLIDO);
        }

        // 4. Generar tokens
        String jwt = jwtUtils.generateToken(usuario);
        String refreshToken = jwtUtils.generateRefreshToken(usuario);

        // 5. Registrar login con Google (éxito)
        loginLogService.registrarLogsUsuario(usuario, Evento.LOGIN_GOOGLE_EXITOSO, Resultado.EXITO, sitio, ip, null);

        return new AuthResponse(jwt, refreshToken, usuario.getRol());
    }

    public AuthResponse refreshToken(String refreshToken, Sitio sitio, String ip) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("El refresh token es requerido");
        }

        String username;
        try {
            username = jwtUtils.extractUsername(refreshToken);
        } catch (Exception e) {
            loginLogService.registrarLogsCorreo("Desconocido", Evento.REFRESH_TOKEN_FALLIDO, Resultado.FALLO, sitio, ip, null);
            throw new SecurityException("Refresh token inválido");
        }

        Usuario usuario = usuarioService.buscarUsuarioByCorreo(username)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        if (!jwtUtils.isRefreshTokenValid(refreshToken, username)) {
            String descripcion = jwtUtils.extractClaims(refreshToken).getExpiration().before(new Date()) ?
                    "Refresh token expirado" : "Refresh token inválido";
            loginLogService.registrarLogsUsuario(usuario, Evento.REFRESH_TOKEN_FALLIDO, Resultado.FALLO, sitio, ip, descripcion);
            throw new SecurityException("Refresh token inválido o expirado. Se requiere iniciar sesión nuevamente.");
        }

        String newJwt = jwtUtils.generateToken(usuario);

        // Registrar éxito de refresh
        loginLogService.registrarLogsUsuario(usuario, Evento.REFRESH_TOKEN_EXITOSO, Resultado.EXITO, sitio, ip, null);

        return new AuthResponse(newJwt, refreshToken, usuario.getRol());
    }

    @Transactional
    public void requestPasswordReset(String email, Sitio sitio, String ip) {
        // --- 0. Revisar bloqueo por IP ---
        if (ipBlockService.estaBloqueada(ip)) {
            loginLogService.registrarLogsCorreo(email, Evento.PASSWORD_RESET_SOLICITUD, Resultado.FALLO, sitio, ip, "0");
            throw new BloqueoException("La IP está bloqueada. Intente más tarde.");
        }

        // --- 1. Buscar usuario ---
        Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioByCorreo(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // --- Revisar bloqueo por cuenta ---
            if (accountBlockService.estaBloqueada(usuario, Evento.PASSWORD_RESET_FALLIDO)) {
                loginLogService.registrarLogsUsuario(usuario, Evento.PASSWORD_RESET_SOLICITUD, Resultado.FALLO, sitio, ip, "1");
                throw new BloqueoException("La cuenta está bloqueada. Intente más tarde.");
            }

            // Registrar intento de reset (aunque sea válido)
            accountBlockService.registrarIntentoFallido(usuario, Evento.PASSWORD_RESET_SOLICITUD_SIN_VERIFICAR, ip);

            // --- Limpiar intentos si ya expiró el bloqueo ---
            accountBlockService.limpiarSiExpirado(usuario, Evento.PASSWORD_RESET_FALLIDO);

            // --- Limpiar tokens antiguos ---
            tokenService.eliminarTokenUsuario(usuario);

            // --- 3. Generar token seguro ---
            String token = tokenService.obtenerToken(usuario);

            // --- Enviar correo ---
            String resetLink = "http://localhost:5173/reset-password?token=" + token;
            emailLogService.notificarUsuarios(usuario, Evento.PASSWORD_RESET_SOLICITUD, Date.from(Instant.now()), resetLink);
            loginLogService.registrarLogsUsuario(usuario, Evento.PASSWORD_RESET_SOLICITUD, Resultado.EXITO, sitio, ip, "2");
        } else {
            // Usuario no encontrado → aun así incrementar contador por IP
            ipBlockService.registrarIntentoFallido(ip);
            loginLogService.registrarLogsCorreo(email, Evento.PASSWORD_RESET_SOLICITUD, Resultado.FALLO, sitio, ip, "1");
        }
    }

    public Usuario verifyResetToken(String token, Sitio sitio, String ip) {
        try {
            // 0. Revisar bloqueo por IP
            if (ipBlockService.estaBloqueada(ip)) {
                loginLogService.registrarLogsCorreo("Desconocido", Evento.PASSWORD_RESET_FALLIDO, Resultado.FALLO, sitio, ip, "0");
                throw new BloqueoException("La IP está bloqueada. Intente más tarde.");
            }

            // 1. Buscar token en la base de datos
            Optional<PasswordResetToken> resetTokenOpt = tokenService.buscarToken(token);

            if (resetTokenOpt.isEmpty()) {
                // Token inválido: registrar intento fallido por IP
                ipBlockService.registrarIntentoFallido(ip);
                loginLogService.registrarLogsCorreo("Desconocido", Evento.PASSWORD_RESET_TOKEN_INVALIDO, Resultado.FALLO, sitio, ip, null);

                throw new IllegalArgumentException("Token inválido");
            }

            PasswordResetToken resetToken = resetTokenOpt.get();
            Usuario usuario = resetToken.getUsuario();

            // --- Revisar bloqueo por cuenta ---
            if (accountBlockService.estaBloqueada(usuario, Evento.PASSWORD_RESET_FALLIDO)) {
                loginLogService.registrarLogsUsuario(usuario, Evento.PASSWORD_RESET_FALLIDO, Resultado.FALLO, sitio, ip, "1");
                throw new BloqueoException("La cuenta está bloqueada. Intente más tarde.");
            }

            // 2. Verificar expiración del token
            if (resetToken.getExpiryDate().before(Date.from(Instant.now()))){
                // Token expirado → eliminarlo y registrar log
                tokenService.eliminarToken(resetToken);

                loginLogService.registrarLogsUsuario(usuario, Evento.PASSWORD_RESET_TOKEN_EXPIRADO, Resultado.FALLO, sitio, ip, null);

                // Registrar intento fallido en cuenta
                accountBlockService.registrarIntentoFallido(usuario, Evento.PASSWORD_RESET_FALLIDO, ip);

                // También incrementar intentos por IP
                ipBlockService.registrarIntentoFallido(ip);

                throw new IllegalArgumentException("Token expirado");
            }

            // --- Token válido: limpiar bloqueos ---
            accountBlockService.limpiarBloqueo(usuario, Evento.PASSWORD_RESET_FALLIDO);
            accountBlockService.limpiarBloqueo(usuario, Evento.PASSWORD_RESET_SOLICITUD_SIN_VERIFICAR);
            ipBlockService.limpiarIntentos(ip);

            // 3. Token válido
            loginLogService.registrarLogsUsuario(usuario, Evento.PASSWORD_RESET_VERIFICADO, Resultado.EXITO, sitio, ip, null);

            return usuario;

        } catch (Exception e) {
            // Captura cualquier excepción inesperada
            loginLogService.registrarLogsCorreo("Desconocido", Evento.PASSWORD_RESET_ERROR, Resultado.FALLO, sitio, ip, "0");
            throw e;
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword, Sitio sitio, String ip) {
        try {
            Usuario usuario = verifyResetToken(token, sitio, ip);

            // --- Validación de fuerza de contraseña ---
            if (PasswordUtils.isWeakPassword(newPassword)) {
                loginLogService.registrarLogsUsuario(usuario, Evento.PASSWORD_RESET_RECHAZADO, Resultado.FALLO, sitio, ip, null);
                return false;
            }

            // --- Guardar nueva contraseña ---;
            usuarioService.restablecerContrasena(usuario, newPassword);

            // --- Eliminar token ---
            tokenService.eliminarTokenUsuario(usuario);

            // --- Enviar correo de confirmación ---
            emailLogService.notificarUsuarios(usuario, Evento.PASSWORD_RESET_EXITOSO, Date.from(Instant.now()), null);
            loginLogService.registrarLogsUsuario(usuario, Evento.PASSWORD_RESET_COMPLETADO, Resultado.EXITO, sitio, ip, null);
            accountBlockService.limpiarBloqueo(usuario, Evento.PASSWORD_RESET_FALLIDO);

            return true;

        } catch (Exception e) {
            loginLogService.registrarLogsCorreo("Desconocido", Evento.PASSWORD_RESET_ERROR, Resultado.FALLO, sitio, ip, "1");
            return false;
        }
    }
}
