package com.Tapia.ProyectoResidencia.Model;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "emails_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idUsuario; // Usuario afectado, si aplica (puede ser null para IP)

    private String correoDestinatario; // A quién se envió el correo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Evento tipoEvento; // LOGIN_FALLIDO, PASSWORD_RESET_FALLIDO, BLOQUEO_IP, etc.

    private String asunto; // Asunto del correo

    @Column(length = 2000)
    private String cuerpo; // Contenido del correo

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEnvio; // Fecha/hora de envío
}
