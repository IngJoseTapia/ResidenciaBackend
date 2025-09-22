package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Model.IpBlock;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.IpBlockRepository;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UsuarioRepository usuarioRepository;
    private final IpBlockRepository ipBlockRepository;
    private final NotificacionService notificacionService;

    private static final int MAX_INTENTOS_USUARIO = 5;
    private static final int MAX_INTENTOS_IP = 5;
    private static final int BLOQUEO_MINUTOS = 15;

    // Manejo de intentos fallidos por usuario
    public void registrarIntentoFallidoUsuario(Usuario usuario) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        if (usuario.getIntentosFallidos() >= MAX_INTENTOS_USUARIO) {
            Date hasta = Date.from(Instant.now().plus(BLOQUEO_MINUTOS, ChronoUnit.MINUTES));
            usuario.setCuentaBloqueadaHasta(hasta);
            notificacionService.notificarBloqueoCuenta(usuario);
        }
        usuarioRepository.save(usuario);
    }

    public void limpiarIntentosUsuario(Usuario usuario) {
        usuario.setIntentosFallidos(0);
        usuario.setCuentaBloqueadaHasta(null);
        usuarioRepository.save(usuario);
    }

    public boolean estaCuentaBloqueada(Usuario usuario) {
        return usuario.getCuentaBloqueadaHasta() != null &&
                usuario.getCuentaBloqueadaHasta().after(new Date());
    }

    // Manejo de intentos fallidos por IP
    public void registrarIntentoFallidoIp(String ip) {
        IpBlock ipBlock = ipBlockRepository.findByIp(ip).orElse(new IpBlock());
        ipBlock.setIp(ip);
        ipBlock.setIntentosFallidos(ipBlock.getIntentosFallidos() + 1);
        if (ipBlock.getIntentosFallidos() >= MAX_INTENTOS_IP) {
            Date hasta = Date.from(Instant.now().plus(BLOQUEO_MINUTOS, ChronoUnit.MINUTES));
            ipBlock.setBloqueadaHasta(hasta);
            notificacionService.notificarBloqueoIp(ip, hasta);
        }
        ipBlockRepository.save(ipBlock);
    }

    public void limpiarIntentosIp(String ip) {
        ipBlockRepository.findByIp(ip).ifPresent(ipBlock -> {
            ipBlock.setIntentosFallidos(0);
            ipBlock.setBloqueadaHasta(null);
            ipBlockRepository.save(ipBlock);
        });
    }

    public boolean estaIpBloqueada(String ip) {
        return ipBlockRepository.findByIp(ip)
                .map(b -> b.getBloqueadaHasta() != null && b.getBloqueadaHasta().after(new Date()))
                .orElse(false);
    }
}
