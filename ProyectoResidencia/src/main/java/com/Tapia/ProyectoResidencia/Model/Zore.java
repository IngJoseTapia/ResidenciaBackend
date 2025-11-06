package com.Tapia.ProyectoResidencia.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(
        name = "zores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"numeracion", "anio"})
)
public class Zore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID autogenerado

    @Min(value = 1, message = "La numeración debe ser mayor que 0")
    @Column(nullable = false)
    private int numeracion;

    @NotBlank(message = "El año es obligatorio")
    @Pattern(
            regexp = "^(19|20)\\d{2}$",
            message = "El año debe tener exactamente 4 dígitos y estar entre 1900 y 2099"
    )
    @Column(nullable = false)
    private String anio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario; // Usuario asignado a esta Zore

    @OneToMany(mappedBy = "zore", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AsignacionZoreAre> asignaciones = new ArrayList<>();
}
