package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.UsuarioEliminado;

public record UsuarioSimple(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno
) {
    public UsuarioSimple(Usuario usuario) {
        this(usuario.getId(), usuario.getNombre(), usuario.getApellidoPaterno(), usuario.getApellidoMaterno());
    }

    // Nuevo constructor para UsuarioEliminado
    public UsuarioSimple(UsuarioEliminado usuarioEliminado) {
        this(usuarioEliminado.getId(), usuarioEliminado.getNombre(), usuarioEliminado.getApellidoPaterno(), usuarioEliminado.getApellidoMaterno());
    }
}
