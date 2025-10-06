package com.Tapia.ProyectoResidencia.Model;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "system_logs")
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idUsuario; // Usuario que ejecutó la acción

    private String correo; // Correo del usuario que ejecutó la acción

    @Enumerated(EnumType.STRING)
    private Rol rol; // Rol del usuario

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaActividad; // Fecha y hora del evento

    @Enumerated(EnumType.STRING)
    private Sitio sitio; // Web o App

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Evento tipoEvento; // Cambios de contraseña, actualización de perfil, etc.

    @Enumerated(EnumType.STRING)
    private Resultado resultado;

    private String descripcion; // Información adicional sobre la acción

    private String ip; // IP desde donde se realizó la acción (opcional)
}
