package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Exception.EmailSendException;
import com.Tapia.ProyectoResidencia.Model.EmailLog;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class EmailLogService {

    private final JavaMailSender mailSender;
    private static final Logger logger = Logger.getLogger(EmailLogService.class.getName());

    @Value("${app.admin.emails}") // lista separada por comas
    private String adminEmails;
    private final EmailLogRepository emailLogRepository;

    public Page<EmailLog> listarLogsCorreo(Pageable pageable) {
        return emailLogRepository.findAllByOrderByFechaEnvioDesc(pageable);
    }

    public void notificarAdministradores(Usuario usuario, Evento evento, Date desbloqueo, String ip){
        String asunto;
        String cuerpo;

        switch (evento) {
            case LOGIN_FALLIDO -> {
                asunto = "[ALERTA] Cuenta bloqueada por intentos fallidos";
                cuerpo = String.format("""
                    La cuenta con correo %s y rol %s ha sido bloqueada temporalmente por múltiples intentos fallidos de inicio de sesión.
                    
                    Tiempo de desbloqueo: %s
                    Ip: %s
                    Fecha y hora de bloqueo: %s
                    """,
                        usuario.getCorreo(),
                        usuario.getRol(),
                        desbloqueo != null ? formatoSoloHora(desbloqueo) : "Desconocido",
                        ip,
                        formatoFechaTexto(Date.from(Instant.now()))
                );
                enviarCorreoAdministradores(usuario, evento, asunto, cuerpo);
            }
            case POSIBLE_ATAQUE_IP -> {
                asunto = "[ALERTA] IP bloqueada por intentos fallidos";
                cuerpo = String.format("""
                    La dirección IP %s ha sido bloqueada temporalmente por múltiples intentos fallidos de inicio de sesión.
                    
                    Tiempo de desbloqueo: %s
                    
                    Fecha y hora de bloqueo: %s
                    """,
                        ip,
                        desbloqueo != null ? formatoSoloHora(desbloqueo) : "Desconocido",
                        formatoFechaTexto(Date.from(Instant.now()))
                );
                enviarCorreoAdministradores(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_RESET_FALLIDO -> {
                asunto = "[ALERTA] Cuenta bloqueada por intentos fallidos de recuperación de contraseña";
                cuerpo = String.format("""
                    La cuenta con correo %s y rol %s ha sido bloqueada temporalmente debido a múltiples intentos fallidos de recuperación de contraseña.
        
                    Tiempo de desbloqueo: %s
                    Ip: %s
                    Fecha y hora de bloqueo: %s
                    """,
                        usuario.getCorreo(),
                        usuario.getRol(),
                        desbloqueo != null ? formatoSoloHora(desbloqueo) : "Desconocido",
                        ip,
                        formatoFechaTexto(Date.from(Instant.now()))
                );
                enviarCorreoAdministradores(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_RESET_EXITOSO -> {
                asunto = "[ALERTA] Bloqueo por exceso de reestablecimientos de contraseña exitosos";
                cuerpo = String.format("""
                    La cuenta con correo %s y rol %s ha sido bloqueada por realizar múltiples reestablecimientos de contraseña en menos de 24 horas.
        
                    Tiempo de desbloqueo: %s
                    Ip: %s
                    Fecha y hora de bloqueo: %s
                    """,
                        usuario.getCorreo(),
                        usuario.getRol(),
                        formatoSoloHora(desbloqueo),
                        ip,
                        formatoFechaTexto(Date.from(Instant.now()))
                );
                enviarCorreoAdministradores(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_CHANGE_RECHAZADO -> {
                asunto = "[ALERTA] Cuenta bloqueada por intentos fallidos de cambio de contraseña";
                cuerpo = String.format("""
                    La cuenta con correo %s y rol %s ha sido bloqueada temporalmente debido a múltiples intentos fallidos de cambio de contraseña.
        
                    Tiempo de desbloqueo: %s
                    Ip: %s
                    Fecha y hora de bloqueo: %s
                    """,
                        usuario.getCorreo(),
                        usuario.getRol(),
                        desbloqueo != null ? formatoSoloHora(desbloqueo) : "Desconocido",
                        ip,
                        formatoFechaTexto(Date.from(Instant.now()))
                );
                enviarCorreoAdministradores(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_CHANGE_EXCESIVOS -> {
                asunto = "[ALERTA] Bloqueo por exceso de cambios de contraseña exitosos";
                cuerpo = String.format("""
                    La cuenta con correo %s y rol %s ha sido bloqueada por realizar múltiples cambios de contraseña en menos de 24 horas.
        
                    Tiempo de desbloqueo: %s
                    Ip: %s
                    Fecha y hora de bloqueo: %s
                    """,
                        usuario.getCorreo(),
                        usuario.getRol(),
                        formatoSoloHora(desbloqueo),
                        ip,
                        formatoFechaTexto(Date.from(Instant.now()))
                );
                enviarCorreoAdministradores(usuario, evento, asunto, cuerpo);
            }
            default -> {
                asunto = "[ALERTA] Cuenta bloqueada";
                cuerpo = String.format("""
                    La cuenta con correo %s y rol %s ha sido bloqueada temporalmente.

                    Tiempo de desbloqueo: %s
                    Ip: %s
                    Fecha y hora de bloqueo: %s
                    """,
                        usuario.getCorreo(),
                        usuario.getRol(),
                        desbloqueo != null ? formatoSoloHora(desbloqueo) : "Desconocido",
                        ip,
                        formatoFechaTexto(Date.from(Instant.now()))
                );
                enviarCorreoAdministradores(usuario, evento, asunto, cuerpo);
            }
        }
    }

    public void notificarUsuarios(Usuario usuario, Evento evento, Date fecha, String token) {
        String asunto;
        String cuerpo;

        switch (evento) {
            case USER_REGISTRADO -> {
                asunto = "[NOTIFICACIÓN] Registro de usuario";
                cuerpo = String.format("""
                        ¡FELICIDADES!
                        Su cuenta ha sido registrada correctamente. Espere la aprobación de un administrador.
                        
                        
                        Fecha y hora de registro: %s
                        """,
                        formatoFechaTexto(fecha)
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case USER_REGISTRADO_GOOGLE -> {
                asunto = "[NOTIFICACIÓN] Registro de usuario mediante GOOGLE";
                cuerpo = String.format("""
                        ¡FELICIDADES!
                        Su cuenta ha sido registrada correctamente vía Google. Espere la aprobación de un administrador.
                        
                        
                        Fecha y hora de registro: %s
                        """,
                        formatoFechaTexto(fecha)
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case LOGIN_FALLIDO -> {
                asunto = "[ALERTA] Cuenta bloqueada";
                cuerpo = String.format("""
                        ¡ATENCIÓN!
                        Estimado usuario %s,su cuenta ha sido bloqueada por demasiados intentos de inicio de sesión.
                        
                        Si usted considera que se trata de un error, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
    
                        Fecha y hora de bloqueo: %s
                        """,
                        usuario.getNombre(),
                        formatoFechaTexto(fecha)
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_RESET_SOLICITUD -> {
                asunto = "[NOTIFICACIÓN] Recuperación de contraseña";
                cuerpo = String.format("""
                        ¡ATENCIÓN!
                        Estimado usuario %s, se ha realizado una solicitud para restablecer su contraseña.
                        
                        Haga clic en el siguiente enlace para continuar:
                        %s
                        
                        El enlace expirará en 15 minutos.
                        
                        
                        Si no ha solicitado restablecer su contraseña, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
                        Fecha y hora de solicitud: %s
                        """,
                        usuario.getNombre(),
                        token,
                        formatoFechaTexto(fecha)
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_RESET_SOLICITUD_SIN_VERIFICAR -> {
                asunto = "[ALERTA] Cuenta bloqueada";
                cuerpo = String.format("""
                        ¡ATENCIÓN!
                        Estimado usuario %s,su cuenta ha sido bloqueada por demasiados intentos de solicitud de reestablecimiento de contraseña.
                        
                        Si usted considera que se trata de un error, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
    
                        Fecha y hora de bloqueo: %s
                        """,
                        usuario.getNombre(),
                        formatoFechaTexto(fecha)
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_RESET_EXITOSO -> {
                asunto = "[NOTIFICACIÓN] Contraseña restablecida correctamente.";
                cuerpo = String.format("""
                        ¡FELICIDADES!
                        Estimado usuario %s, su contraseña ha sido restablecida exitosamente.
                        
                        Si usted no ha solicitado ni ha actualizado su contraseña, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
                        
                        Fecha y hora del cambio: %s
                        """,
                        usuario.getNombre(),
                        formatoFechaTexto(fecha)
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_RESET_FALLIDO -> {
                asunto = "[ALERTA] Cuenta bloqueada";
                cuerpo = String.format("""
                        ¡ATENCIÓN!
                        Estimado usuario %s, su cuenta ha sido bloqueada por demasiados intentos de recuperación de contraseña.
                        
                        Si usted cree que se trata de un error, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
    
                        Fecha y hora de bloqueo: %s
                        """,
                        usuario.getNombre(),
                        fecha != null ? formatoFechaTexto(fecha) : "Desconocido"
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_CHANGE_RECHAZADO -> {
                asunto = "[ALERTA] Cuenta bloqueada";
                cuerpo = String.format("""
                        ¡ATENCIÓN!
                        Estimado usuario %s, su cuenta ha sido bloqueada por demasiados intentos fallidos de cambio de contraseña.
                        
                        Si usted cree que se trata de un error, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
    
                        Fecha y hora de bloqueo: %s
                        """,
                        usuario.getNombre(),
                        fecha != null ? formatoFechaTexto(fecha) : "Desconocido"
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_CHANGE_EXCESIVOS -> {
                asunto = "[ALERTA] Cuenta bloqueada";
                cuerpo = String.format("""
                        ¡ATENCIÓN!
                        Estimado usuario %s, su cuenta ha sido bloqueada por demasiados cambios de contraseña en menos de 24 horas.
                        
                        Si usted cree que se trata de un error, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
    
                        Fecha y hora de bloqueo: %s
                        """,
                        usuario.getNombre(),
                        fecha != null ? formatoFechaTexto(fecha) : "Desconocido"
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            case PASSWORD_CHANGE_EXITOSO -> {
                asunto = "[NOTIFICACIÓN] Contraseña actualizada correctamente.";
                cuerpo = String.format("""
                        ¡FELICIDADES!
                        Estimado usuario %s, su contraseña ha sido actualizada exitosamente.
                        
                        Si usted no ha solicitado ni ha actualizado su contraseña, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
                        
                        Fecha y hora del cambio: %s
                        """,
                        usuario.getNombre(),
                        formatoFechaTexto(fecha)
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
            default -> {
                asunto = "[ALERTA] Se ha detectado actividad inusual en su cuenta";
                cuerpo = String.format("""
                        ¡ATENCIÓN!
                        Estimado usuario %s, se ha detectado actividad inusual en su cuenta, verifique que todo se encuentre en orden.
                        
                        Si usted cree que se trata de un error, informe inmediatamente a su Jefe y a Soporte para recibir ayuda al respecto.
                        
    
                        Fecha y hora de notificación: %s
                        """,
                        usuario.getNombre(),
                        fecha != null ? formatoFechaTexto(fecha) : "Desconocido"
                );
                enviarCorreoUsuarios(usuario, evento, asunto, cuerpo);
            }
        }
    }

    private void enviarCorreoAdministradores(Usuario usuario, Evento evento, String asunto, String cuerpo) {
        for (String destinatario : adminEmails.split(",")) {
            try {
                SimpleMailMessage msgAdmin = new SimpleMailMessage();
                msgAdmin.setTo(destinatario.trim());
                msgAdmin.setSubject("[ADMIN] " + asunto);

                // Determinar el contenido del correo
                if (evento == Evento.POSIBLE_ATAQUE_IP) {
                    msgAdmin.setText(cuerpo);
                } else {
                    msgAdmin.setText(cuerpo + "\n\nUsuario afectado: " + usuario.getCorreo());
                }

                // Enviar correo
                mailSender.send(msgAdmin);

                // Registrar notificación exitosa
                if (evento == Evento.POSIBLE_ATAQUE_IP) {
                    registrarNotificacion(null, destinatario.trim(), evento, "[ADMIN] " + asunto, cuerpo);
                } else {
                    registrarNotificacion(usuario.getId(), destinatario.trim(), evento, "[ADMIN] " + asunto, cuerpo + "\n\nUsuario afectado: " + usuario.getCorreo());
                }

            } catch (Exception ex) {
                // Registrar fallo individualmente
                registrarNotificacion(usuario.getId(), destinatario.trim(), evento, "[ADMIN] " + asunto, "ERROR AL ENVIAR CORREO: " + ex.getMessage() + "\n\n" + cuerpo);
                EmailSendException e = new EmailSendException("No se pudo enviar correo a " + usuario.getCorreo(), ex);
                logger.log(Level.WARNING, "Error enviando correo al admin {0}: {1}",
                        new Object[]{destinatario.trim(), e.getMessage()});
                //throw new RuntimeException("Error enviando correo a " + usuario.getCorreo(), ex);
            }
        }
    }

    private void enviarCorreoUsuarios(Usuario usuario, Evento evento, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(usuario.getCorreo());
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);

            // Enviar correo
            mailSender.send(mensaje);

            // Registrar notificación exitosa
            registrarNotificacion(usuario.getId(), usuario.getCorreo(), evento, asunto, cuerpo);

        } catch (Exception e) {
            // Registrar fallo y lanzar excepción
            registrarNotificacion(usuario.getId(), usuario.getCorreo(), evento, asunto, "ERROR AL ENVIAR CORREO: " + e.getMessage() + "\n\n" + cuerpo);
            EmailSendException ex = new EmailSendException("No se pudo enviar correo a " + usuario.getCorreo(), e);
            logger.log(Level.WARNING, ex.getMessage());
        }
    }

    private void registrarNotificacion(Long idUsuario, String correoDestinatario, Evento tipoEvento,
                                       String asunto, String cuerpo) {
        EmailLog log = new EmailLog();
        log.setIdUsuario(idUsuario);
        log.setCorreoDestinatario(correoDestinatario);
        log.setTipoEvento(tipoEvento);
        log.setAsunto(asunto);
        log.setCuerpo(cuerpo);
        log.setFechaEnvio(Date.from(Instant.now()));
        emailLogRepository.save(log);
    }

    private String formatoFechaTexto(Date fecha) {
        if (fecha == null) return "Desconocido";
        // Patrones:
        // dd -> día con dos dígitos
        // MMMM -> nombre completo del mes
        // yyyy -> año completo
        // HH:mm:ss -> hora en formato 24 horas
        SimpleDateFormat sdf = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm:ss", new Locale("es", "MX"));
        return sdf.format(fecha);
    }

    private String formatoSoloHora(Date fecha) {
        if (fecha == null) return "Desconocido";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(fecha);
    }
}
