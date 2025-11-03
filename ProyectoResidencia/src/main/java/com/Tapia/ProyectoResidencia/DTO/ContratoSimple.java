package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Model.Contrato;

public record ContratoSimple(
        Long id,
        String puesto
) {
    public ContratoSimple(Contrato contrato) {
        this(contrato.getId(), contrato.getPuesto());
    }
}
