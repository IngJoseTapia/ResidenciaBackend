package com.Tapia.ProyectoResidencia.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "notificacion_usuario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notificacion_id", "usuario_id"})
)
public class NotificacionUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "notificacion_id", nullable = false)
    private Notification notificacion;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(nullable = false)
    private boolean resuelta = false;

    private LocalDateTime fechaLectura;
}
