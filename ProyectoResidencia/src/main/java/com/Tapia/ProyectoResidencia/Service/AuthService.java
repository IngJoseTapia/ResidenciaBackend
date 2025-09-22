package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.LoginRequest;
import com.Tapia.ProyectoResidencia.DTO.RegisterRequest;
import com.Tapia.ProyectoResidencia.DTO.AuthResponse;
import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Enum.Status;
import com.Tapia.ProyectoResidencia.Exception.BloqueoException;
import com.Tapia.ProyectoResidencia.Model.Login;
import com.Tapia.ProyectoResidencia.Model.PasswordResetToken;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.LoginRepository;
import com.Tapia.ProyectoResidencia.Repository.PasswordResetTokenRepository;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import com.Tapia.ProyectoResidencia.Security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final LoginRepository loginRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final NotificacionService notificacionService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetAttemptService passwordResetAttemptService;

    public AuthService(UsuarioRepository usuarioRepository,
                       LoginRepository loginRepository,
                       PasswordResetTokenRepository tokenRepository,
                       NotificacionService notificacionService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       LoginAttemptService loginAttemptService,
                       PasswordResetAttemptService passwordResetAttemptService) {
        this.usuarioRepository = usuarioRepository;
        this.loginRepository = loginRepository;
        this.tokenRepository = tokenRepository;
        this.notificacionService = notificacionService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
        this.passwordResetAttemptService = passwordResetAttemptService;
    }

    public String register(RegisterRequest request) {
        // 1. Validaciones de negocio mínimas
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        if (usuarioRepository.existsByCorreo(request.email())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        // 2. Mapear DTO a entidad Usuario
        Usuario usuario = new Usuario();
        usuario.setCorreo(request.email());
        usuario.setContrasena(passwordEncoder.encode(request.password()));
        usuario.setNombre(request.nombre());
        usuario.setApellidoPaterno(request.apellidoPaterno());
        usuario.setApellidoMaterno(request.apellidoMaterno());
        usuario.setGenero(request.genero());
        usuario.setTelefono(request.telefono());
        usuario.setRol(Rol.USER);
        usuario.setStatus(Status.PENDIENTE); // Registro híbrido: requiere aprobación
        usuario.setFechaRegistro(new Date());
        usuario.setIntentosFallidos(0);
        usuario.setCuentaBloqueadaHasta(null);

        // 3. Guardar en la base de datos
        // Si algún campo viola las validaciones del modelo, Hibernate lanzará ConstraintViolationException
        usuarioRepository.save(usuario);

        // Registrar log de registro (opcional)
        registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), Sitio.WEB,
                "Éxito", "Usuario registrado", Evento.USER_REGISTRADO, null);

        return "Usuario registrado correctamente. Espera la aprobación de un administrador.";
    }

    public AuthResponse login(LoginRequest request, Sitio sitio, String ip) {
        Usuario usuario = null;
        Login loginLog = new Login();
        loginLog.setCorreo(request.email());
        loginLog.setFechaActividad(new Date());
        loginLog.setSitio(sitio);
        loginLog.setIp(ip);

        try {
            // 0. Revisar bloqueo por IP
            if (loginAttemptService.estaIpBloqueada(ip)) {
                registrarLog(request.email(), null, Rol.DESCONOCIDO, sitio,
                        "Fallo", "IP bloqueada temporalmente por múltiples intentos fallidos",
                        Evento.LOGIN_FALLIDO, ip);
                throw new BloqueoException("La IP está bloqueada. Intente más tarde.");
            }

            // 1. Verificar si el correo existe en la BD
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(request.email());
            if (usuarioOpt.isEmpty()) {
                // incrementar contador por IP
                loginAttemptService.registrarIntentoFallidoIp(ip);

                registrarLog(request.email(), null, Rol.DESCONOCIDO, sitio,
                        "Fallo", "Inicio de sesión fallido: correo no registrado",
                        Evento.LOGIN_FALLIDO, ip);
                throw new NoSuchElementException("El correo no está registrado");
            }

            usuario = usuarioOpt.get();
            loginLog.setIdUsuario(usuario.getId());
            loginLog.setRol(usuario.getRol());

            // 2. Revisar bloqueo por cuenta
            if (loginAttemptService.estaCuentaBloqueada(usuario)) {
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio,
                        "Fallo", "Cuenta bloqueada temporalmente por múltiples intentos fallidos",
                        Evento.LOGIN_FALLIDO, ip);
                throw new BloqueoException("La cuenta está bloqueada. Intente más tarde.");
            } else {
                // Si había bloqueo expirado, limpiar
                if (usuario.getCuentaBloqueadaHasta() != null && usuario.getCuentaBloqueadaHasta().before(new Date())) {
                    loginAttemptService.limpiarIntentosUsuario(usuario);
                }
            }

            // 3. Autenticar (puede lanzar BadCredentialsException)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // 4. Login exitoso: resetear contadores y generar tokens
            loginAttemptService.limpiarIntentosUsuario(usuario);
            loginAttemptService.limpiarIntentosIp(ip);

            String jwt = jwtUtil.generateToken(usuario);
            String refreshToken = jwtUtil.generateRefreshToken(usuario);

            // Registrar log de éxito
            registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio,
                    "Éxito", "Inicio de sesión exitoso", Evento.LOGIN_EXITOSO, ip);

            return new AuthResponse(jwt, refreshToken);
        } catch (BadCredentialsException e) {
            //  Contraseña incorrecta
            if (usuario != null) {
                loginAttemptService.registrarIntentoFallidoUsuario(usuario); // incrementa y bloquea si aplica
            }
            loginAttemptService.registrarIntentoFallidoIp(ip);

            registrarLog(request.email(), usuario != null ? usuario.getId() : null,
                    usuario != null ? usuario.getRol() : Rol.DESCONOCIDO,
                    sitio, "Fallo", "Inicio de sesión fallido: contraseña incorrecta",
                    Evento.LOGIN_FALLIDO, ip);

            throw new BadCredentialsException("Contraseña incorrecta");
        }
    }

    public AuthResponse loginWithGoogle(String email, String nombre, Sitio sitio, String ip) {
        // 0. Revisar bloqueo por IP
        if (loginAttemptService.estaIpBloqueada(ip)) {
            registrarLog(email, null, Rol.DESCONOCIDO, sitio,
                    "Fallo", "IP bloqueada temporalmente por múltiples intentos fallidos",
                    Evento.LOGIN_FALLIDO, ip);
            throw new BloqueoException("La IP está bloqueada. Intente más tarde.");
        }

        // 1. Validar parámetros
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo proporcionado por Google no es válido");
        }

        // 2. Buscar o crear usuario
        Usuario usuario = usuarioRepository.findByCorreo(email).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setCorreo(email);
            u.setNombre(nombre != null ? nombre : "Usuario Google");
            u.setApellidoPaterno("N/A");
            u.setApellidoMaterno("N/A");
            u.setGenero("Otro");
            u.setTelefono("0000000000");
            u.setRol(Rol.USER);
            u.setStatus(Status.PENDIENTE);
            u.setFechaRegistro(new Date());
            u.setIntentosFallidos(0);
            u.setCuentaBloqueadaHasta(null);
            usuarioRepository.save(u);
            return u;
        });

        // 3. Revisar bloqueo por cuenta
        if (loginAttemptService.estaCuentaBloqueada(usuario)) {
            registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio,
                    "Fallo", "Cuenta bloqueada temporalmente por múltiples intentos fallidos",
                    Evento.LOGIN_FALLIDO, ip);
            throw new BloqueoException("La cuenta está bloqueada. Intente más tarde.");
        } else {
            // Limpiar bloqueo expirado si aplica
            if (usuario.getCuentaBloqueadaHasta() != null && usuario.getCuentaBloqueadaHasta().before(new Date())) {
                loginAttemptService.limpiarIntentosUsuario(usuario);
            }
        }

        // 4. Generar tokens
        String jwt = jwtUtil.generateToken(usuario);
        String refreshToken = jwtUtil.generateRefreshToken(usuario);

        // 5. Registrar login con Google (éxito)
        registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio,
                "Éxito", "Inicio de sesión con Google exitoso",
                Evento.LOGIN_GOOGLE_EXITOSO, ip);

        return new AuthResponse(jwt, refreshToken);
    }

    public AuthResponse refreshToken(String refreshToken, Sitio sitio, String ip) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("El refresh token es requerido");
        }

        String username;
        try {
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            registrarLog("Desconocido", null, Rol.DESCONOCIDO, sitio,
                    "Fallo", "Refresh token inválido", Evento.REFRESH_TOKEN_FALLIDO, ip);
            throw new SecurityException("Refresh token inválido");
        }

        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        if (!jwtUtil.isRefreshTokenValid(refreshToken, username)) {
            String descripcion = jwtUtil.extractClaims(refreshToken).getExpiration().before(new Date()) ?
                    "Refresh token expirado" : "Refresh token inválido";
            registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(),
                    sitio, "Fallo", descripcion, Evento.REFRESH_TOKEN_FALLIDO, ip);
            throw new SecurityException("Refresh token inválido o expirado. Se requiere iniciar sesión nuevamente.");
        }

        String newJwt = jwtUtil.generateToken(usuario);

        // Registrar éxito de refresh
        registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(),
                sitio, "Éxito", "Refresh token exitoso", Evento.REFRESH_TOKEN_EXITOSO, ip);

        return new AuthResponse(newJwt, refreshToken);
    }

    @Transactional
    public void requestPasswordReset(String email, Sitio sitio, String ip) {

        // --- 1. Rate limit por IP ---
        if (passwordResetAttemptService.isBlocked(email, ip)) {
            registrarLog(email, null, Rol.DESCONOCIDO, sitio,
                    "Fallo", "Intento de recuperación bloqueado por límite de solicitudes",
                    Evento.PASSWORD_RESET_SOLICITUD, ip);
            return; // No procesar
        }

        passwordResetAttemptService.registerAttempt(email, ip);

        // --- 2. Buscar usuario ---
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // --- Limpiar tokens antiguos ---
            tokenRepository.deleteByUsuario(usuario);

            // --- 3. Generar token seguro ---
            SecureRandom secureRandom = new SecureRandom();
            byte[] tokenBytes = new byte[32]; // 256 bits
            secureRandom.nextBytes(tokenBytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUsuario(usuario);
            resetToken.setExpiryDate(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)));
            tokenRepository.save(resetToken);

            // --- Enviar correo ---
            String resetLink = "http://localhost:5173/reset-password?token=" + token;

            notificacionService.enviarCorreo(usuario.getCorreo(),
                    "Recuperación de contraseña",
                    "Has solicitado restablecer tu contraseña.\n\n" +
                            "Haz clic en el siguiente enlace para continuar:\n" + resetLink + "\n\n" +
                            "Este enlace expirará en 15 minutos.");

            registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(),
                    sitio, "Éxito", "Solicitud de cambio de contraseña enviada",
                    Evento.PASSWORD_RESET_SOLICITUD, ip);
        } else {
            // Usuario no encontrado: no hacemos nada visible para el cliente
            registrarLog(email, null, Rol.DESCONOCIDO, sitio,
                    "Aviso", "Solicitud de recuperación para correo no registrado",
                    Evento.PASSWORD_RESET_SOLICITUD, ip);
        }
    }

    public Usuario verifyResetToken(String token, Sitio sitio, String ip) {
        try {
            Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(token);
            if (resetTokenOpt.isEmpty() || resetTokenOpt.get().getExpiryDate().before(new Date())) {
                registrarLog("Desconocido", null, Rol.DESCONOCIDO, sitio,
                        "Fallo", "Token de recuperación inválido o expirado",
                        Evento.PASSWORD_RESET_VERIFICADO, ip);
                throw new IllegalArgumentException("Token inválido o expirado"); // Se maneja internamente
            }

            Usuario usuario = resetTokenOpt.get().getUsuario();
            registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(),
                    sitio, "Éxito", "Token de recuperación verificado",
                    Evento.PASSWORD_RESET_VERIFICADO, ip);

            return usuario;

        } catch (Exception e) {
            registrarLog("Desconocido", null, Rol.DESCONOCIDO, sitio,
                    "Fallo", "Error al verificar token", Evento.PASSWORD_RESET_VERIFICADO, ip);
            throw e;
        }
    }


    @Transactional
    public boolean resetPassword(String token, String newPassword, Sitio sitio, String ip) {
        try {
            Usuario usuario = verifyResetToken(token, sitio, ip);

            // --- Validación de fuerza de contraseña ---
            if (!isPasswordStrong(newPassword)) {
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(),
                        sitio, "Fallo", "Contraseña no cumple criterios de seguridad",
                        Evento.PASSWORD_RESET_COMPLETADO, ip);
                return false;
            }

            // --- Guardar nueva contraseña ---
            usuario.setContrasena(passwordEncoder.encode(newPassword));
            usuarioRepository.save(usuario);

            // --- Eliminar token ---
            tokenRepository.deleteByUsuario(usuario);

            // --- Enviar correo de confirmación ---
            notificacionService.enviarCorreo(usuario.getCorreo(),
                    "Contraseña cambiada",
                    "Tu contraseña ha sido actualizada correctamente.");

            registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(),
                    sitio, "Éxito", "Contraseña actualizada exitosamente",
                    Evento.PASSWORD_RESET_COMPLETADO, ip);

            return true;

        } catch (Exception e) {
            registrarLog("Desconocido", null, Rol.DESCONOCIDO, sitio,
                    "Fallo", "Error al actualizar contraseña", Evento.PASSWORD_RESET_COMPLETADO, ip);
            return false;
        }
    }

    // Metodo auxiliar para validar fuerza de contraseña
    private boolean isPasswordStrong(String password) {
        if (password == null) return false;
        // Al menos 8 caracteres, una mayúscula, una minuscula, un número, un símbolo
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    }

    private void registrarLog(String correo, Long idUsuario, Rol rol, Sitio sitio,
                              String resultado, String descripcion, Evento tipoEvento, String ip) {
        Login log = new Login();
        log.setCorreo(correo != null ? correo : "Desconocido");
        log.setIdUsuario(idUsuario);
        log.setRol(rol != null ? rol : Rol.DESCONOCIDO);
        log.setSitio(sitio != null ? sitio : Sitio.WEB);
        log.setResultado(resultado);
        log.setDescripcion(descripcion);
        log.setTipoEvento(tipoEvento != null ? tipoEvento : Evento.PRUEBAS);
        log.setFechaActividad(new Date());
        log.setIp(ip);
        loginRepository.save(log);
    }
}
