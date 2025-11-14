package com.Tapia.ProyectoResidencia.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Set;

@Entity
@Table(
        name = "secciones",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"numero_seccion", "anio"})
        }
)
@Data
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El número de sección es obligatorio")
    @Pattern(regexp = "^[0-9]{4}$", message = "El número de sección debe tener 4 dígitos")
    @Column(name = "numero_seccion", length = 4, nullable = false)
    private String numeroSeccion;

    @NotBlank(message = "El año es obligatorio")
    @Pattern(
            regexp = "^(19|20)\\d{2}$",
            message = "El año debe tener exactamente 4 dígitos y estar entre 1900 y 2099"
    )
    @Column(nullable = false)
    private String anio;

    // 🔹 Relación con Asignación Zore-Are
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_zore_are_id", nullable = false)
    private AsignacionZoreAre asignacionZoreAre;

    // 🔹 Relación con localidades (muchas a muchas)
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "seccion_localidad",
            joinColumns = @JoinColumn(name = "seccion_id"),
            inverseJoinColumns = @JoinColumn(name = "localidad_id")
    )
    private Set<Localidad> localidades;
}
