package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Model.AccountBlock;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.AccountBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountBlockService {

    private final AccountBlockRepository accountBlockRepository;
    private final EmailLogService emailLogService;

    // Parámetros configurables por evento
    private static final int MAX_INTENTOS_LOGIN = 5;                // login fallido → bloqueo rápido
    private static final int MAX_INTENTOS_RESET_SOLICITUD = 3;      // solicitudes de reset → evita spam de correos
    private static final int MAX_INTENTOS_RESET_TOKEN = 5;          // validaciones de token → evita fuerza bruta
    private static final int MAX_INTENTOS_CHANGE_FAIL = 3;          // fallos en cambios internos
    private static final int MAX_CAMBIOS_PASSWORD = 2;              // máx. 2 cambios en 24h
    private static final int VENTANA_HORAS = 24;                    // ventana de control
    private static final int BLOQUEO_MINUTOS = 15;                  // tiempo estándar de bloqueo

    //Verifica si el usuario está actualmente bloqueado
    public boolean estaBloqueada(Usuario usuario, Evento evento) {
        return accountBlockRepository.findByUsuarioAndEvento(usuario, evento)
                .map(bloqueo -> bloqueo.getBloqueadaHasta() != null && bloqueo.getBloqueadaHasta().after(new Date()))
                .orElse(false);
    }

    //Registra un intento fallido de login y bloquea si excede el límite
    public void registrarIntentoFallido(Usuario usuario, Evento evento, String ip) {
        AccountBlock bloqueo = accountBlockRepository
                .findByUsuarioAndEvento(usuario, evento)
                .orElseGet(() -> {
                    AccountBlock nuevo = new AccountBlock();
                    nuevo.setUsuario(usuario);
                    nuevo.setEvento(evento);
                    nuevo.setIntentosFallidos(0);
                    nuevo.setIp(ip);
                    return nuevo;
                });

        //Incrementar intentos acumulados
        int nuevosIntentos = bloqueo.getIntentosFallidos() + 1;
        bloqueo.setIntentosFallidos(nuevosIntentos);

        //Retomar el límite de intentos según el evento
        int limite = obtenerLimiteIntentos(evento);

        //Comparar los intentos actuales vs. el límite de intentos
        if (nuevosIntentos >= limite) {
            //Si se ha superado el límite de intentos, bloquear la cuenta y registrar según el evento
            Date hasta = new Date(System.currentTimeMillis() + BLOQUEO_MINUTOS * 60 * 1000);
            bloqueo.setBloqueadaHasta(hasta);
            accountBlockRepository.save(bloqueo);
            // 👉 Notificación al usuario según el tipo de evento
            emailLogService.notificarAdministradores(usuario, evento, hasta, ip);
            emailLogService.notificarUsuarios(usuario, evento, Date.from(Instant.now()), null);
        } else {
            accountBlockRepository.save(bloqueo);
        }
    }

    //Registra un cambio de contraseña exitoso
    //Controla la ventana de 24 h para limitar cambios excesivos
    public void registrarCambioPasswordExitoso(Usuario usuario, Evento evento, String ip) {
        AccountBlock bloqueo = obtenerOBuild(usuario, evento, ip);
        Date ahora = new Date();

        if (bloqueo.getBloqueadaHasta() != null && bloqueo.getBloqueadaHasta().before(ahora)) {
            bloqueo.setIntentosFallidos(0);
            bloqueo.setBloqueadaHasta(null);
        }

        int cambios = bloqueo.getIntentosFallidos() + 1;
        bloqueo.setIntentosFallidos(cambios);

        if (cambios >= MAX_CAMBIOS_PASSWORD) {
            bloqueo.setBloqueadaHasta(Date.from(Instant.now().plus(VENTANA_HORAS, ChronoUnit.HOURS)));
            emailLogService.notificarAdministradores(usuario, Evento.PASSWORD_CHANGE_EXCESIVOS, bloqueo.getBloqueadaHasta(), ip);
            emailLogService.notificarUsuarios(usuario, Evento.PASSWORD_CHANGE_EXCESIVOS, Date.from(Instant.now()), null);
        }

        accountBlockRepository.save(bloqueo);
    }

    //Devuelve el límite de intentos según el tipo de evento
    private int obtenerLimiteIntentos(Evento evento) {
        return switch (evento) {
            case LOGIN_FALLIDO -> MAX_INTENTOS_LOGIN;
            case PASSWORD_RESET_SOLICITUD_SIN_VERIFICAR -> MAX_INTENTOS_RESET_SOLICITUD;
            case PASSWORD_RESET_FALLIDO -> MAX_INTENTOS_RESET_TOKEN;
            case PASSWORD_CHANGE_RECHAZADO -> MAX_INTENTOS_CHANGE_FAIL;
            default -> MAX_INTENTOS_LOGIN; // fallback genérico
        };
    }

    //Limpia el bloqueo de la cuenta (se usa en login exitoso o cuando expira)
    public void limpiarBloqueo(Usuario usuario, Evento evento) {
        accountBlockRepository.findByUsuarioAndEvento(usuario, evento)
                .ifPresent(bloqueo -> {
                    bloqueo.setIntentosFallidos(0);
                    bloqueo.setBloqueadaHasta(null);
                    accountBlockRepository.save(bloqueo);
                });
    }

    //Si la fecha de bloqueo ya expiró, limpia el registro
    public void limpiarSiExpirado(Usuario usuario, Evento evento) {
        accountBlockRepository.findByUsuarioAndEvento(usuario, evento)
                .ifPresent(bloqueo -> {
                    if (bloqueo.getBloqueadaHasta() != null &&
                            bloqueo.getBloqueadaHasta().before(new Date())) {
                        bloqueo.setIntentosFallidos(0);
                        bloqueo.setBloqueadaHasta(null);
                        accountBlockRepository.save(bloqueo);
                    }
                });
    }

    //Factor común para obtener o crear un registro de bloqueo
    private AccountBlock obtenerOBuild(Usuario usuario, Evento evento, String ip) {
        return accountBlockRepository.findByUsuarioAndEvento(usuario, evento)
                .orElseGet(() -> {
                    AccountBlock nuevo = new AccountBlock();
                    nuevo.setUsuario(usuario);
                    nuevo.setEvento(evento);
                    nuevo.setIntentosFallidos(0);
                    nuevo.setIp(ip);
                    return nuevo;
                });
    }

    public Optional<AccountBlock> obtenerBloqueoActivo(Usuario usuario, Evento evento) {
        Optional<AccountBlock> bloqueoOpt = accountBlockRepository.findByUsuarioAndEvento(usuario, evento);
        bloqueoOpt.ifPresent(bloqueo -> {
            if (bloqueo.getBloqueadaHasta() != null && bloqueo.getBloqueadaHasta().before(new Date())) {
                bloqueo.setIntentosFallidos(0);
                bloqueo.setBloqueadaHasta(null);
                accountBlockRepository.save(bloqueo);
            }
        });
        return bloqueoOpt;
    }
}
