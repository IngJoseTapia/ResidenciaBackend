package com.Tapia.ProyectoResidencia.Model;

import com.Tapia.ProyectoResidencia.Enum.StatusContrato;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "usuario_contratos",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = "numero_contrato")
    }
)
public class UsuarioContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Relación con Usuario (activo) ---
    // Puede ser nulo si el usuario fue eliminado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    @JsonIgnore
    private Usuario usuario;

    // --- Relación con UsuarioEliminado ---
    // Puede ser nulo si el usuario aún está activo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_eliminado_id", referencedColumnName = "id")
    @JsonIgnore
    private UsuarioEliminado usuarioEliminado;

    // --- Relación con Contrato ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", referencedColumnName = "id", nullable = false)
    private Contrato contrato;

    // --- Número de contrato (clave sensible) ---
    @NotBlank(message = "El número de contrato es obligatorio.")
    @Size(min = 30, max = 50, message = "El número de contrato debe tener entre 30 y 50 caracteres.")
    @Column(name = "numero_contrato", nullable = false, unique = true, length = 50)
    private String numeroContrato;

    // --- Fecha de asignación ---
    @CreationTimestamp
    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    // --- Estado del contrato ---
    @NotNull(message = "El estado del contrato es obligatorio.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusContrato estado;

    // --- Observaciones opcionales ---
    @Column(columnDefinition = "TEXT")
    private String observaciones;

}
