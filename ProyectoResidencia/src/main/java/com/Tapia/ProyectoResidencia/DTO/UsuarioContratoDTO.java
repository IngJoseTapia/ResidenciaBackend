package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.StatusContrato;
import com.Tapia.ProyectoResidencia.Model.UsuarioContrato;

import java.time.LocalDateTime;

public record UsuarioContratoDTO(
        Long id,
        UsuarioSimple usuario,
        UsuarioSimple usuarioEliminado,
        ContratoSimple contrato,
        String numeroContrato,
        LocalDateTime fechaAsignacion,
        StatusContrato status,
        String observaciones
) {
    public UsuarioContratoDTO(UsuarioContrato uc) {
        this(
                uc.getId(),
                uc.getUsuario() != null ? new UsuarioSimple(uc.getUsuario()) : null,
                uc.getUsuarioEliminado() != null ? new UsuarioSimple(uc.getUsuarioEliminado()) : null,
                new ContratoSimple(uc.getContrato()),
                uc.getNumeroContrato(),
                uc.getFechaAsignacion(),
                uc.getEstado(),
                uc.getObservaciones()
        );
    }
}
