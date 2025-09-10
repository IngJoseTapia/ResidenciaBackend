package com.Tapia.ProyectoResidencia.Model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;

@Data
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tipo;
    private String codigo;
    private Date fechaInicio;
    private Date fechaConclusion;
    private String actividadesGenericas;
    private String descripcion;
}
