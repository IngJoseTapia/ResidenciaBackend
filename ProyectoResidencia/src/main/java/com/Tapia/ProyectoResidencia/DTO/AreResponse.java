package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Model.Are;

public record AreResponse(
        Long id,
        int numeracion,
        String anio,
        UsuarioSimple usuario
) {
    public record UsuarioSimple(
            Long id,
            String nombre,
            String apellidoPaterno,
            String apellidoMaterno
    ) {}

    // 🔹 Constructor auxiliar para mapear desde la entidad
    public AreResponse(Are a) {
        this(
                a.getId(),
                a.getNumeracion(),
                a.getAnio(),
                a.getUsuario() != null
                        ? new UsuarioSimple(
                        a.getUsuario().getId(),
                        a.getUsuario().getNombre(),
                        a.getUsuario().getApellidoPaterno(),
                        a.getUsuario().getApellidoMaterno()
                )
                        : null
        );
    }
}
