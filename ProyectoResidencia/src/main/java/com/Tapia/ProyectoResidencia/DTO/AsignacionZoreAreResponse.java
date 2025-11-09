package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Model.AsignacionZoreAre;

public record AsignacionZoreAreResponse(
        Long id,
        String anio,
        ZoreSimple zore,
        AreSimple are
) {
    public record ZoreSimple(
            Long id,
            int numeracion,
            String nombreUsuario
    ) {}

    public record AreSimple(
            Long id,
            int numeracion,
            String nombreUsuario
    ) {}

    public AsignacionZoreAreResponse(AsignacionZoreAre asignacion) {
        this(
                asignacion.getId(),
                asignacion.getZore().getAnio(),
                new ZoreSimple(
                        asignacion.getZore().getId(),
                        asignacion.getZore().getNumeracion(),
                        asignacion.getZore().getUsuario() != null
                                ? asignacion.getZore().getUsuario().getNombre() + " " +
                                asignacion.getZore().getUsuario().getApellidoPaterno() + " " +
                                asignacion.getZore().getUsuario().getApellidoMaterno()
                                : "—"
                ),
                new AreSimple(
                        asignacion.getAre().getId(),
                        asignacion.getAre().getNumeracion(),
                        asignacion.getAre().getUsuario() != null
                                ? asignacion.getAre().getUsuario().getNombre() + " " +
                                asignacion.getAre().getUsuario().getApellidoPaterno() + " " +
                                asignacion.getAre().getUsuario().getApellidoMaterno()
                                : "—"
                )
        );
    }
}
