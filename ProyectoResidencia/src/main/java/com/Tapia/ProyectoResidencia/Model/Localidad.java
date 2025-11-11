package com.Tapia.ProyectoResidencia.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Entity
@Table(
        name = "localidades",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"numero_localidad", "municipio_id"})
        }
)
@Data
public class Localidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID generado automáticamente

    @NotBlank(message = "El número de localidad no puede estar vacío")
    @Size(max = 10, message = "El número de localidad no debe exceder los 10 caracteres")
    @Column(name = "numero_localidad", length = 10, nullable = false)
    private String numeroLocalidad;

    @NotBlank(message = "El nombre de la localidad no puede estar vacío")
    @Size(max = 100, message = "El nombre no debe exceder los 100 caracteres")
    @Column(length = 100, nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "municipio_id", nullable = false)
    @JsonBackReference
    private Municipio municipio;
}
