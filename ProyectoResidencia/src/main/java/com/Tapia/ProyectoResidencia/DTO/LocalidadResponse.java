package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Model.Localidad;

public record LocalidadResponse(
        Long id,
        String numeroLocalidad,
        String nombre,
        String municipioId,
        String municipioNombre
) {
    public LocalidadResponse(Localidad localidad) {
        this(
                localidad.getId(),
                localidad.getNumeroLocalidad(),
                localidad.getNombre(),
                localidad.getMunicipio() != null ? localidad.getMunicipio().getId() : null,
                localidad.getMunicipio() != null ? localidad.getMunicipio().getNombre() : null
        );
    }
}
