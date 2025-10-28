package com.Tapia.ProyectoResidencia.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "contratos")
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El puesto es obligatorio")
    @Column(nullable = false)
    private String puesto; // Nombre del puesto o cargo

    @NotBlank(message = "El código es obligatorio")
    @Column(nullable = false, unique = true)
    private String codigo; // Código de identificación del puesto

    @NotBlank(message = "El nivel tabular es obligatorio")
    @Column(nullable = false)
    private String nivelTabular; // Nivel tabular del puesto

    @NotNull(message = "La fecha de inicio es obligatorio")
    @Column(nullable = false)
    private LocalDate fechaInicio; //Fecha de inicio del cargo

    @NotNull(message = "La fecha de conclusión es obligatorio")
    @Column(nullable = false)
    private LocalDate fechaConclusion; // Fecha de conclusión del cargo

    @NotBlank(message = "Las actividades genéricas son obligatorias")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String actividadesGenericas; // Actividad o función generica del puesto

    @NotNull(message = "El sueldo es obligatorio")
    @DecimalMin(value = "0.01", message = "El sueldo debe ser mayor que 0")
    @Digits(integer = 6, fraction = 2, message = "El sueldo debe tener máximo 6 dígitos enteros y 2 decimales")
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal sueldo;

}
