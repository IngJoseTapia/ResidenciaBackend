package com.Tapia.ProyectoResidencia.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "vocalias")
public class Vocalia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String abreviatura;

    @Column(nullable = false, length = 100)
    private String nombreCompleto;

    @OneToMany(mappedBy = "vocalia")
    @JsonIgnore
    private List<Usuario> usuarios = new ArrayList<>();
}

