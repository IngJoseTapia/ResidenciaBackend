package com.Tapia.ProyectoResidencia.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Entity
@Table(name = "municipios")
@Data
public class Municipio {

    @Id
    @Column(length = 3, nullable = false, unique = true)
    @Pattern(regexp = "^[0-9]{3}$", message = "El ID debe tener exactamente 3 dígitos")
    private String id; // Ejemplo: "075", "101"

    @NotBlank
    @Size(max = 100)
    private String nombre;
}

