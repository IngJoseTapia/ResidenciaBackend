package com.Tapia.ProyectoResidencia.Model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "asignaciones_zore_are",
        uniqueConstraints = @UniqueConstraint(columnNames = {"zore_id", "are_id"})
)
public class AsignacionZoreAre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zore_id", nullable = false)
    private Zore zore;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "are_id", nullable = false)
    private Are are;
}
