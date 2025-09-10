package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class NotificacionService {

    private final JavaMailSender mailSender;

    @Value("${app.admin.emails}") // lista separada por comas
    private String adminEmails;

    public NotificacionService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notificarBloqueoCuenta(Usuario usuario) {
        String asunto = "[ALERTA] Cuenta bloqueada por intentos fallidos";
        String mensaje = String.format("""
                La cuenta con correo %s y rol %s ha sido bloqueada temporalmente por múltiples intentos fallidos de inicio de sesión.
                
                Tiempo de desbloqueo: %s
                """,
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getCuentaBloqueadaHasta()
        );
        enviarCorreo(asunto, mensaje);
    }

    public void notificarBloqueoIp(String ip, Date bloqueadaHasta) {
        String asunto = "[ALERTA] IP bloqueada por intentos fallidos";
        String mensaje = String.format("""
                La dirección IP %s ha sido bloqueada temporalmente por múltiples intentos fallidos de inicio de sesión.
                
                Tiempo de desbloqueo: %s
                """,
                ip,
                bloqueadaHasta
        );
        enviarCorreo(asunto, mensaje);
    }

    private void enviarCorreo(String asunto, String cuerpo) {
        for (String destinatario : adminEmails.split(",")) {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario.trim());
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
        }
    }
}
