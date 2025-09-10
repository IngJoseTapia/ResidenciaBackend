package com.Tapia.ProyectoResidencia.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "ip_blocks")
public class IpBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip; // Dirección IP bloqueada

    private int intentosFallidos; // Número de intentos fallidos

    @Temporal(TemporalType.TIMESTAMP)
    private Date bloqueadaHasta; // Fecha/hora hasta la cual la IP estará bloqueada
}
