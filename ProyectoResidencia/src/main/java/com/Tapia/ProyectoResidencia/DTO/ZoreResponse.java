package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Model.Zore;

public record ZoreResponse(
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
    public ZoreResponse(Zore z) {
        this(
                z.getId(),
                z.getNumeracion(),
                z.getAnio(),
                z.getUsuario() != null
                        ? new UsuarioSimple(
                        z.getUsuario().getId(),
                        z.getUsuario().getNombre(),
                        z.getUsuario().getApellidoPaterno(),
                        z.getUsuario().getApellidoMaterno()
                )
                        : null
        );
    }
}
