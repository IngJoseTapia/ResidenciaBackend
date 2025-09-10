package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.LoginRequest;
import com.Tapia.ProyectoResidencia.DTO.RegisterRequest;
import com.Tapia.ProyectoResidencia.DTO.AuthResponse;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Enum.Status;
import com.Tapia.ProyectoResidencia.Model.IpBlock;
import com.Tapia.ProyectoResidencia.Model.Login;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.IpBlockRepository;
import com.Tapia.ProyectoResidencia.Repository.LoginRepository;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import com.Tapia.ProyectoResidencia.Security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final LoginRepository loginRepository;
    private final IpBlockRepository ipBlockRepository;
    private final NotificacionService notificacionService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository,
                       LoginRepository loginRepository,
                       IpBlockRepository ipBlockRepository,
                       NotificacionService notificacionService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.loginRepository = loginRepository;
        this.ipBlockRepository = ipBlockRepository;
        this.notificacionService = notificacionService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public String register(RegisterRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (request.confirmPassword() == null || request.confirmPassword().isBlank()) {
            throw new IllegalArgumentException("Confirmar contraseña es obligatorio");
        }
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (request.apellidoPaterno() == null || request.apellidoPaterno().isBlank()) {
            throw new IllegalArgumentException("El apellido paterno es obligatorio");
        }
        if (request.apellidoMaterno() == null || request.apellidoMaterno().isBlank()) {
            throw new IllegalArgumentException("El apellido materno es obligatorio");
        }
        if (request.telefono() == null || request.telefono().isBlank()) {
            throw new IllegalArgumentException("El telefono es obligatorio");
        }
        if (request.genero() == null || request.genero().isBlank()) {
            throw new IllegalArgumentException("El genero es obligatorio");
        }

        if (usuarioRepository.existsByCorreo(request.email())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!request.email().matches(emailRegex)) {
            throw new IllegalArgumentException("El correo no tiene un formato válido");
        }


        // Validar que la contraseña y confirmPassword coincidan
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        // Validación de la contraseña según tu patrón (puedes usar regex o BCrypt)
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!request.password().matches(regex)) {
            throw new IllegalArgumentException("La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial (@$!%*?&)");
        }

        String nombreRegex = "^[A-Za-zÀ-ÿ\\s]+$";
        if (!request.nombre().matches(nombreRegex)) {
            throw new IllegalArgumentException("El nombre contiene caracteres inválidos");
        }
        if (!request.apellidoPaterno().matches(nombreRegex)) {
            throw new IllegalArgumentException("El apellido paterno contiene caracteres inválidos");
        }
        if (!request.apellidoMaterno().matches(nombreRegex)) {
            throw new IllegalArgumentException("El apellido materno contiene caracteres inválidos");
        }

        if (!List.of("Masculino", "Femenino", "Otro").contains(request.genero())) {
            throw new IllegalArgumentException("El género debe ser Masculino, Femenino u Otro");
        }

        if (!request.telefono().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("El teléfono debe tener exactamente 10 dígitos");
        }


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

        usuarioRepository.save(usuario);
        return "Usuario registrado correctamente. Espera la aprobación de un administrador.";
    }

    public AuthResponse login(LoginRequest request, Sitio sitio, String ip) {
        Login loginLog = new Login();
        loginLog.setCorreo(request.email());
        loginLog.setFechaActividad(new Date());
        loginLog.setSitio(sitio);
        loginLog.setIp(ip);

        // 0. Validar si la IP ya está bloqueada
        Optional<IpBlock> ipBlockOpt = ipBlockRepository.findByIp(ip);
        if (ipBlockOpt.isPresent()) {
            IpBlock ipBlock = ipBlockOpt.get();
            if (ipBlock.getBloqueadaHasta() != null && ipBlock.getBloqueadaHasta().after(new Date())) {
                loginLog.setResultado("Fallo");
                loginLog.setDescripcion("IP bloqueada temporalmente por múltiples intentos fallidos");
                loginLog.setRol(Rol.DESCONOCIDO);
                loginRepository.save(loginLog);
                throw new RuntimeException("La IP está bloqueada. Intente más tarde.");
            } else if (ipBlock.getBloqueadaHasta() != null && ipBlock.getBloqueadaHasta().before(new Date())) {
                // El bloqueo ya expiró → resetear
                ipBlock.setIntentosFallidos(0);
                ipBlock.setBloqueadaHasta(null);
                ipBlockRepository.save(ipBlock);
            }
        }

        // 1. Verificar si el correo existe en la BD
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(request.email());
        if (usuarioOpt.isEmpty()) {
            // Incrementar contador por IP
            registrarIntentoFallidoIp(ip);

            loginLog.setIdUsuario(null);
            loginLog.setRol(Rol.DESCONOCIDO);
            loginLog.setResultado("Fallo");
            loginLog.setDescripcion("Inicio de sesión fallido: el correo no está registrado en el sistema");
            loginRepository.save(loginLog);

            throw new NoSuchElementException("El correo no está registrado");
        }

        Usuario usuario = usuarioOpt.get();
        loginLog.setIdUsuario(usuario.getId());
        loginLog.setRol(usuario.getRol());

        // 2. Validar si el usuario está bloqueado
        if (usuario.getCuentaBloqueadaHasta() != null) {
            if (usuario.getCuentaBloqueadaHasta().after(new Date())) {
                // Todavía está en el periodo de bloqueo
                loginLog.setResultado("Fallo");
                loginLog.setDescripcion("Cuenta bloqueada temporalmente por múltiples intentos fallidos");
                loginRepository.save(loginLog);
                throw new RuntimeException("La cuenta está bloqueada. Intente más tarde.");
            } else {
                // El tiempo de bloqueo ya expiró → desbloquear
                usuario.setCuentaBloqueadaHasta(null);
                usuario.setIntentosFallidos(0);
                usuarioRepository.save(usuario);
            }
        }

        try {
            // 3. Validar credenciales
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // Resetear intentos fallidos al iniciar sesión correctamente
            usuario.setIntentosFallidos(0);
            usuario.setCuentaBloqueadaHasta(null);
            usuarioRepository.save(usuario);

            limpiarIntentosIp(ip);

            // 4. Generar tokens
            String jwt = jwtUtil.generateToken(usuario);
            String refreshToken = jwtUtil.generateRefreshToken(usuario);

            loginLog.setResultado("Éxito");
            loginLog.setDescripcion("Inicio de sesión exitoso");
            loginRepository.save(loginLog);

            return new AuthResponse(jwt, refreshToken);

        } catch (BadCredentialsException e) {
            // Contraseña incorrecta → aumentar contador de usuario
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);

            if (usuario.getIntentosFallidos() >= 5) {
                usuario.setCuentaBloqueadaHasta(Date.from(
                        Instant.now().plus(15, ChronoUnit.MINUTES) // bloqueado 15 minutos
                ));
                // Notificar a administradores
                notificacionService.notificarBloqueoCuenta(usuario);
            }
            usuarioRepository.save(usuario);

            // También registrar intento por IP
            registrarIntentoFallidoIp(ip);

            loginLog.setResultado("Fallo");
            loginLog.setDescripcion("Inicio de sesión fallido: contraseña incorrecta");
            loginRepository.save(loginLog);

            throw new BadCredentialsException("Contraseña incorrecta");
        }
    }

    public AuthResponse loginWithGoogle(String email, String nombre) {
        // 1. Validar parámetros
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo proporcionado por Google no es válido");
        }

        // 2. Buscar al usuario en la BD
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(email);

        Usuario usuario;
        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();
        } else {
            // 3. Si no existe, registrarlo automáticamente
            usuario = new Usuario();
            usuario.setCorreo(email);
            usuario.setNombre(nombre != null ? nombre : "Usuario Google");
            usuario.setApellidoPaterno("N/A"); // puedes pedir completar datos luego
            usuario.setApellidoMaterno("N/A");
            usuario.setGenero("Otro");
            usuario.setTelefono("0000000000"); // placeholder
            usuario.setRol(Rol.USER);
            usuario.setStatus(Status.PENDIENTE); // con Google lo activamos directo
            usuario.setFechaRegistro(new Date());
            usuario.setIntentosFallidos(0);
            usuario.setCuentaBloqueadaHasta(null);

            usuarioRepository.save(usuario);
        }

        // 4. Generar tokens
        String jwt = jwtUtil.generateToken(usuario);
        String refreshToken = jwtUtil.generateRefreshToken(usuario);

        return new AuthResponse(jwt, refreshToken);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("El refresh token es requerido");
        }

        String username;
        try {
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new SecurityException("Refresh token inválido");
        }

        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        if (!jwtUtil.isTokenValid(refreshToken, username)) {
            throw new SecurityException("Refresh token inválido o expirado. Se requiere iniciar sesión nuevamente.");
        }

        String newJwt = jwtUtil.generateToken(usuario);
        return new AuthResponse(newJwt, refreshToken);
    }



    private void registrarIntentoFallidoIp(String ip) {
        IpBlock ipBlock = ipBlockRepository.findByIp(ip).orElse(new IpBlock());
        ipBlock.setIp(ip);
        ipBlock.setIntentosFallidos(ipBlock.getIntentosFallidos() + 1);

        if (ipBlock.getIntentosFallidos() >= 5) {
            Date hasta = Date.from(Instant.now().plus(15, ChronoUnit.MINUTES));
            ipBlock.setBloqueadaHasta(hasta);
            notificacionService.notificarBloqueoIp(ip, hasta);
        }

        ipBlockRepository.save(ipBlock);
    }

    private void limpiarIntentosIp(String ip) {
        ipBlockRepository.findByIp(ip).ifPresent(ipBlock -> {
            ipBlock.setIntentosFallidos(0);
            ipBlock.setBloqueadaHasta(null);
            ipBlockRepository.save(ipBlock);
        });
    }

}
