package com.Tapia.ProyectoResidencia.Model;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "login_logs")
public class Login {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //Id del registro

    private Long idUsuario; //Id del usuario que esta haciendo la accion (puede ser null para inicios de sesion fallidos)

    private String correo; //Correo con el que se inició sesión

    @Enumerated(EnumType.STRING)
    private Rol rol; // Rol del usuario (puede ser null si el usuario no existe)

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaActividad; //fecha y hora en que se inició sesión

    @Enumerated(EnumType.STRING)
    private Sitio sitio; //Sitio de origen del inicio de sesión, si es desde la web o de la app

    private String resultado; //Si se logro iniciar sesión o si hubo error en la contraseña, o se ingreso con un correo que no existe

    private String descripcion; //Alguna posible anotación extra

    private String ip;//Ip de origen desde donde se esta realizando la accion (esto es opcional, si es posible recuperar la ip)

    @Enumerated(EnumType.STRING) @Column(nullable = false) private Evento tipoEvento; //Para poder identificar que tipo de evento se ejecutó
}
