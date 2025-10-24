package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.UsuarioEliminado;
import com.Tapia.ProyectoResidencia.Repository.UsuarioEliminadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioEliminadoService {

    private final UsuarioEliminadoRepository usuarioEliminadoRepository;

    public UsuarioEliminado guardarRegistro(Usuario usuario){
        UsuarioEliminado eliminado = new UsuarioEliminado();
        eliminado.setIdOriginal(usuario.getId());
        eliminado.setCorreo(usuario.getCorreo());
        eliminado.setNombre(usuario.getNombre());
        eliminado.setApellidoPaterno(usuario.getApellidoPaterno());
        eliminado.setApellidoMaterno(usuario.getApellidoMaterno());
        eliminado.setFechaCreacion(usuario.getFechaRegistro());
        return guardar(eliminado);
    }

    private UsuarioEliminado guardar(UsuarioEliminado usuarioEliminado) {
        return usuarioEliminadoRepository.save(usuarioEliminado);
    }
}
