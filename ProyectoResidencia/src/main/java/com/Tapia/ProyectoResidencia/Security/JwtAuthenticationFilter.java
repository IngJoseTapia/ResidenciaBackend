package com.Tapia.ProyectoResidencia.Security;

import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Model.Login;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.LoginRepository;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final LoginRepository loginRepository;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService,
                                   LoginRepository loginRepository, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.loginRepository = loginRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String path = request.getServletPath();

        // 🔹 Ignorar endpoints públicos
        if (path.startsWith("/auth/login") ||
                path.startsWith("/auth/register") ||
                path.startsWith("/auth/refresh") ||
                path.startsWith("/auth/google")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String username = jwtUtil.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.isTokenValid(token, username)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                } else {
                    registrarTokenFallido(request, "Token inválido o expirado");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o expirado");
                    return;
                }
            }

        } catch (ExpiredJwtException e) {
            logger.warn("Token expirado", e);
            registrarTokenFallido(request, "Token expirado");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expirado");
            return;
        } catch (Exception e) {
            logger.error("Error en autenticación JWT", e);
            registrarTokenFallido(request, "JWT token inválido");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT Token inválido");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void registrarTokenFallido(HttpServletRequest request, String descripcion) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String username;
        Usuario usuario = null;
        try {
            username = jwtUtil.extractUsername(token);
            usuario = usuarioRepository.findByCorreo(username).orElse(null);
        } catch (Exception ignored) {
            username = "Desconocido";
        }

        registrarLogin(
                usuario != null ? usuario.getCorreo() : "Desconocido",
                usuario != null ? usuario.getId() : null,
                usuario != null ? usuario.getRol() : Rol.DESCONOCIDO,
                request.getRemoteAddr(),
                Sitio.WEB,
                "Fallo",
                descripcion
        );
    }

    private void registrarLogin(String correo, Long idUsuario, Rol rol, String ip, Sitio sitio,
                                String resultado, String descripcion) {
        Login log = new Login();
        log.setCorreo(correo);
        log.setIdUsuario(idUsuario);
        log.setRol(rol);
        log.setFechaActividad(new Date());
        log.setIp(ip);
        log.setSitio(sitio);
        log.setResultado(resultado);
        log.setDescripcion(descripcion);
        loginRepository.save(log);
    }
}
