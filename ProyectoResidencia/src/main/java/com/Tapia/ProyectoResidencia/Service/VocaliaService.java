package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.VocaliaCreate;
import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.Vocalia;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import com.Tapia.ProyectoResidencia.Repository.VocaliaRepository;
import com.Tapia.ProyectoResidencia.Utils.AuthUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class VocaliaService {

    private final VocaliaRepository vocaliaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SystemLogService systemLogService;
    private final UsuarioService usuarioService;

    public List<Vocalia> listarTodas() {
        return vocaliaRepository.findAll();
    }

    @Transactional
    public Vocalia crear(Authentication authentication, VocaliaCreate dto, Sitio sitio, String ip) {
        Vocalia vocalia = new Vocalia();
        vocalia.setAbreviatura(dto.abreviatura());
        vocalia.setNombreCompleto(dto.nombreCompleto());
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario user = usuarioService.getUsuarioEntityByCorreo(correo);

        // Crear
        validarDuplicado(dto.abreviatura(), null, user, Evento.VOCALIA_REGISTER_FALLIDO, sitio, ip);

        return vocalia(vocalia, Evento.VOCALIA_REGISTER_EXITOSO, user, sitio, ip);
    }

    @Transactional
    public void actualizar(Authentication authentication, Long id, VocaliaCreate dto, Sitio sitio,  String ip) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario user = usuarioService.getUsuarioEntityByCorreo(correo);

        Vocalia v = vocaliaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vocalía no encontrada"));

        // Actualizar
        validarDuplicado(dto.abreviatura(), id, user, Evento.VOCALIA_UPDATE_FALLIDO, sitio, ip);

        v.setAbreviatura(dto.abreviatura());
        v.setNombreCompleto(dto.nombreCompleto());

        vocalia(v, Evento.VOCALIA_UPDATE_EXITOSO, user, sitio, ip);
    }

    @Transactional
    public void eliminar(Authentication authentication, Long id, Sitio sitio, String ip) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario user = usuarioService.getUsuarioEntityByCorreo(correo);

        Vocalia vocalia = vocaliaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vocalía no encontrada"));

        Evento eventoExito = Evento.VOCALIA_DELETE_EXITOSO;
        Evento eventoError = Evento.VOCALIA_DELETE_ERROR;

        try {
            vocaliaRepository.delete(vocalia);
            systemLogService.registrarLogUsuario(user, eventoExito, Resultado.EXITO, sitio, ip, null);
        } catch (DataIntegrityViolationException e) {
            String msg = "No se puede eliminar la vocalía porque está referenciada en otros registros.";
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (OptimisticLockingFailureException e) {
            String msg = "Conflicto de concurrencia al eliminar registro de Vocalía.";
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (TransactionSystemException e) {
            Throwable root = e.getRootCause();
            String msg = (root instanceof ConstraintViolationException)
                    ? "Violación de restricción al eliminar vocalía: " + root.getMessage()
                    : "Error en la transacción de la base de datos al eliminar vocalía.";
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (Exception e) {
            String msg = "Error inesperado al eliminar vocalía: " + e.getMessage();
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;
        }
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

    private Vocalia vocalia(Vocalia vocalia, Evento evento, Usuario user, Sitio sitio, String ip) {
        Evento eventoError = switch (evento) {
            case VOCALIA_REGISTER_EXITOSO -> Evento.VOCALIA_REGISTER_ERROR;
            case VOCALIA_UPDATE_EXITOSO -> Evento.VOCALIA_UPDATE_ERROR;
            case VOCALIA_DELETE_EXITOSO -> Evento.VOCALIA_DELETE_ERROR;
            default -> Evento.DESCONOCIDO;
        };

        try {
            Vocalia v = vocaliaRepository.save(vocalia);
            systemLogService.registrarLogUsuario(user, evento, Resultado.EXITO, sitio, ip, null);
            return v;
        } catch (DataIntegrityViolationException e) {
            String msg = "Violación de integridad en la BD: " + e.getMostSpecificCause().getMessage();
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (OptimisticLockingFailureException e) {
            String msg = "Conflicto de concurrencia al actualizar registro de Vocalía.";
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (TransactionSystemException e) {
            Throwable root = e.getRootCause();
            String msg = (root instanceof ConstraintViolationException)
                    ? "Violación de restricción de validación: " + root.getMessage()
                    : "Error en la transacción de la base de datos.";
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (Exception e) {
            String msg = "Error inesperado al guardar o actualizar Vocalía: " + e.getMessage();
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;
        }
    }

    private void validarDuplicado(String abreviatura, Long id, Usuario usuario, Evento evento, Sitio sitio, String ip) {
        boolean duplicado = (id == null)
                ? vocaliaRepository.existsByAbreviatura(abreviatura) // para creación
                : vocaliaRepository.existsByAbreviaturaAndIdNot(abreviatura, id); // para actualización

        if (duplicado) {
            systemLogService.registrarLogUsuario(usuario, evento, Resultado.FALLO, sitio, ip, null);
            throw new IllegalArgumentException("La abreviatura ya existe");
        }
    }
}
