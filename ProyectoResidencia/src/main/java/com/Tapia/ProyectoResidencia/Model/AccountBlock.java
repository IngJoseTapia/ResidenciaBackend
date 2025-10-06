package com.Tapia.ProyectoResidencia.Model;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "account_blocks")
public class AccountBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private Evento evento; // LOGIN_FALLIDO, PASSWORD_RESET_FALLIDO, etc.

    private int intentosFallidos;

    @Temporal(TemporalType.TIMESTAMP)
    private Date bloqueadaHasta;

    private String ip; // opcional, para ligar IP y usuario
}
