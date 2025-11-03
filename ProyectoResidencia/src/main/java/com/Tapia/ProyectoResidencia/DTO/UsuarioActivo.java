package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Model.Usuario;

public record UsuarioActivo(
        Long id,
        String nombreCompleto
) {
    public static UsuarioActivo fromEntity(Usuario usuario) {
        String nombreCompleto = String.format("%s %s %s",
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno()
        ).trim();

        return new UsuarioActivo(usuario.getId(), nombreCompleto);
    }
}
