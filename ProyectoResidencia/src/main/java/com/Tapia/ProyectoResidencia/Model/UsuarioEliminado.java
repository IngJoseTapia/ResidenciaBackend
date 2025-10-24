package com.Tapia.ProyectoResidencia.Model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Data
@Table(name = "usuarios_eliminados")
public class UsuarioEliminado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idOriginal;

    @Column(nullable = false)
    private String correo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellidoPaterno;

    @Column(nullable = false)
    private String apellidoMaterno;

    private Date fechaCreacion;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaEliminacion;

    @PrePersist
    public void prePersist() {
        if (fechaEliminacion == null) {
            fechaEliminacion = LocalDateTime.now();
        }
    }

    // Opcional: agregar el rol y el status previo (para auditoría)
    //private String rolAnterior;
    //private String statusAnterior;
}