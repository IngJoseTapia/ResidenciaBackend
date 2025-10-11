package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.Vocalia;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import com.Tapia.ProyectoResidencia.Repository.VocaliaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class VocaliaService {

    private final VocaliaRepository vocaliaRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Vocalia> listarTodas() {
        return vocaliaRepository.findAll();
    }

    @Transactional
    public Vocalia crear(Vocalia vocalia) {
        if (vocaliaRepository.existsByAbreviatura(vocalia.getAbreviatura())) {
            throw new IllegalArgumentException("La abreviatura ya existe");
        }
        return vocaliaRepository.save(vocalia);
    }

    @Transactional
    public Vocalia actualizar(Long id, Vocalia vocaliaActualizada) {
        Vocalia v = vocaliaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vocalía no encontrada"));
        v.setAbreviatura(vocaliaActualizada.getAbreviatura());
        v.setNombreCompleto(vocaliaActualizada.getNombreCompleto());
        return vocaliaRepository.save(v);
    }

    @Transactional
    public void eliminar(Long id) {
        Vocalia vocalia = vocaliaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vocalía no encontrada"));
        vocaliaRepository.delete(vocalia);
    }

    @Transactional
    public Usuario asignarVocaliaAUsuario(Long usuarioId, Long vocaliaId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        Vocalia vocalia = vocaliaRepository.findById(vocaliaId)
                .orElseThrow(() -> new NoSuchElementException("Vocalía no encontrada"));

        if (!usuario.getRol().equals(Rol.VOCAL)) {
            throw new IllegalArgumentException("Solo usuarios con rol VOCAL pueden ser asignados a una vocalía");
        }

        usuario.setVocalia(vocalia);
        return usuarioRepository.save(usuario);
    }
}
