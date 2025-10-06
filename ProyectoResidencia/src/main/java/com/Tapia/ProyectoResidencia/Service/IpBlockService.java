package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Model.IpBlock;
import com.Tapia.ProyectoResidencia.Repository.IpBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class IpBlockService {

    private final IpBlockRepository ipBlockRepository;
    private final EmailLogService emailLogService;

    private static final int MAX_INTENTOS_IP = 5;
    private static final int BLOQUEO_MINUTOS = 15;

    public void registrarIntentoFallido(String ip) {
        IpBlock ipBlock = ipBlockRepository.findByIp(ip).orElse(new IpBlock());
        ipBlock.setIp(ip);
        ipBlock.setIntentosFallidos(ipBlock.getIntentosFallidos() + 1);

        if (ipBlock.getIntentosFallidos() >= MAX_INTENTOS_IP) {
            Date hasta = Date.from(Instant.now().plus(BLOQUEO_MINUTOS, ChronoUnit.MINUTES));
            ipBlock.setBloqueadaHasta(hasta);
            emailLogService.notificarAdministradores(null, Evento.POSIBLE_ATAQUE_IP, hasta, ip);
        }

        ipBlockRepository.save(ipBlock);
    }

    public boolean estaBloqueada(String ip) {
        return ipBlockRepository.findByIp(ip)
                .map(b -> b.getBloqueadaHasta() != null && b.getBloqueadaHasta().after(new Date()))
                .orElse(false);
    }

    public void limpiarIntentos(String ip) {
        ipBlockRepository.findByIp(ip).ifPresent(ipBlock -> {
            ipBlock.setIntentosFallidos(0);
            ipBlock.setBloqueadaHasta(null);
            ipBlockRepository.save(ipBlock);
        });
    }
}
