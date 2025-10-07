package com.Tapia.ProyectoResidencia.Security;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import com.Tapia.ProyectoResidencia.Service.LoginLogService;
import com.Tapia.ProyectoResidencia.Utils.IpUtils;
import com.Tapia.ProyectoResidencia.Utils.JwtUtils;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final LoginLogService loginLogService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService,
                                   UsuarioRepository usuarioRepository,
                                   LoginLogService loginLogService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.loginLogService = loginLogService;
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

        String ip = IpUtils.extractClientIp(request);

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String username = jwtUtils.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtils.isTokenValid(token, username)) {
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
                    registrarTokenFallido(request, "Token inválido o expirado", ip);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o expirado");
                    return;
                }
            }

        } catch (ExpiredJwtException e) {
            logger.warn("Token expirado", e);
            registrarTokenFallido(request, "Token expirado", ip);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expirado");
            return;
        } catch (Exception e) {
            logger.error("Error en autenticación JWT", e);
            registrarTokenFallido(request, "JWT token inválido", ip);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT Token inválido");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void registrarTokenFallido(HttpServletRequest request, String descripcion, String ip) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String username;
        Usuario usuario;
        try {
            username = jwtUtils.extractUsername(token);
            usuario = usuarioRepository.findByCorreo(username).orElse(null);
            loginLogService.registrarLogsUsuario(usuario, Evento.TOKEN_ERROR_INTERNO_VALIDACION, Resultado.FALLO, Sitio.WEB, ip, descripcion);
        } catch (Exception ignored) {
            username = "Desconocido";
            loginLogService.registrarLogsCorreo(username, Evento.TOKEN_ERROR_INTERNO_VALIDACION, Resultado.FALLO, Sitio.WEB, ip, descripcion);
        }
    }
}
