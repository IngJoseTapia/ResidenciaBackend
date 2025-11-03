package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.UsuarioContratoCreate;
import com.Tapia.ProyectoResidencia.DTO.UsuarioContratoDTO;
import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.UsuarioContratoCreationException;
import com.Tapia.ProyectoResidencia.Model.Contrato;
import com.Tapia.ProyectoResidencia.Model.UsuarioContrato;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.UsuarioEliminado;
import com.Tapia.ProyectoResidencia.Repository.UsuarioContratoRepository;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioContratoService {

    private final UsuarioContratoRepository usuarioContratoRepository;
    private final UsuarioService usuarioService;
    private final ContratoService contratoService;
    private final SystemLogService systemLogService;

    // --- Crear nueva vinculación ---
    @Transactional
    public UsuarioContrato asignarContrato(UsuarioContratoCreate dto, Authentication auth, Sitio sitio, String ip) {
        // Obtener usuario autenticado
        String correoAuth = auth.getName();
        Usuario usuarioAuth = usuarioService.getUsuarioEntityByCorreo(correoAuth);

        // Validar que el número de contrato no exista
        if (usuarioContratoRepository.existsByNumeroContrato(dto.numeroContrato())) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.ASIGNAR_CONTRATO_FALLIDO, Resultado.FALLO, sitio, ip, "1");
            throw new UsuarioContratoCreationException("El número de contrato ya está asignado a otro usuario.", HttpStatus.CONFLICT);
        }

        Usuario usuario = usuarioService.getUsuarioById(dto.usuarioId());
        Contrato contrato = contratoService.obtenerPorId(dto.contratoId());

        // Evitar asignar el mismo contrato dos veces al mismo usuario
        if (usuarioContratoRepository.existsByUsuarioAndContrato_Id(usuario, dto.contratoId())) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.ASIGNAR_CONTRATO_FALLIDO, Resultado.FALLO, sitio, ip, "2");
            throw new UsuarioContratoCreationException("El usuario ya tiene asignado este contrato.", HttpStatus.CONFLICT);
        }

        UsuarioContrato uc = new UsuarioContrato();
        uc.setUsuario(usuario);
        uc.setUsuarioEliminado(null);
        uc.setContrato(contrato);
        uc.setNumeroContrato(dto.numeroContrato());
        uc.setEstado(dto.status());
        uc.setObservaciones(dto.observaciones());

        try {
            UsuarioContrato usuarioContrato = usuarioContratoRepository.save(uc);
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.ASIGNAR_CONTRATO_EXITOSO, Resultado.EXITO, sitio, ip, "Usuario: " + usuario.getCorreo() + ", Contrato: " + contrato.getPuesto());
            return usuarioContrato;
        } catch (DataIntegrityViolationException e) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.ASIGNAR_CONTRATO_ERROR, Resultado.FALLO, sitio, ip, e.getMostSpecificCause().getMessage());
            throw new UsuarioContratoCreationException("Violación de integridad: " + e.getMostSpecificCause().getMessage(), HttpStatus.CONFLICT);
        } catch (TransactionSystemException e) {
            Throwable root = e.getRootCause();
            String msg = (root instanceof ConstraintViolationException)
                    ? "Violación de restricción: " + root.getMessage()
                    : "Error en la transacción de base de datos";
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.ASIGNAR_CONTRATO_ERROR, Resultado.FALLO, sitio, ip, msg);
            throw new UsuarioContratoCreationException(msg, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.ASIGNAR_CONTRATO_ERROR, Resultado.FALLO, sitio, ip, e.getMessage());
            throw e;
        }
    }

    // --- Obtener todos los contratos ---
    public Page<UsuarioContratoDTO> obtenerUsuariosContratosPaginadoDTO(Pageable pageable) {
        Page<UsuarioContrato> page = usuarioContratoRepository.findAll(pageable);

        var dtos = page.stream()
                .map(UsuarioContratoDTO::new)
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // --- Obtener contratos por usuario activo ---
    public List<UsuarioContrato> obtenerPorUsuario(Usuario usuario) {
        return usuarioContratoRepository.findByUsuario(usuario);
    }

    // --- Obtener contratos por usuario eliminado ---
    public List<UsuarioContrato> obtenerPorUsuarioEliminado(UsuarioEliminado usuarioEliminado) {
        return usuarioContratoRepository.findByUsuarioEliminado(usuarioEliminado);
    }

    // --- Obtener por ID ---
    public Optional<UsuarioContrato> obtenerPorId(Long id) {
        return usuarioContratoRepository.findById(id);
    }

    // --- Actualizar contrato ---
    @Transactional
    public void actualizarVinculoContrato(Long id, UsuarioContratoCreate dto, Authentication auth, Sitio sitio, String ip) {
        // Obtener usuario autenticado
        String correoAuth = auth.getName();
        Usuario usuarioAuth = usuarioService.getUsuarioEntityByCorreo(correoAuth);

        Optional<UsuarioContrato> optionalVinculo = usuarioContratoRepository.findById(id);
        if (optionalVinculo.isEmpty()) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.UPDATE_VINCULO_CONTRATO_FALLIDO, Resultado.FALLO, sitio, ip, "1");
            throw new UsuarioContratoCreationException("No se ha encontrado la vinculación del contrato con el usuario", HttpStatus.NOT_FOUND);
        }
        UsuarioContrato usuarioContrato = optionalVinculo.get();

        // 🔹 Validar que el usuario (activo) exista
        Usuario usuario = usuarioService.getUsuarioById(dto.usuarioId());

        //🔹 Validar que el contrato exista
        Contrato contrato = contratoService.obtenerPorId(dto.contratoId());

        // Validar que el número de contrato no esté repetido en otro registro
        if (usuarioContratoRepository.existsByNumeroContratoAndIdNot(dto.numeroContrato(), id)) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.UPDATE_VINCULO_CONTRATO_FALLIDO, Resultado.FALLO, sitio, ip, "2");
            throw new UsuarioContratoCreationException("El número de contrato ya pertenece a otra vinculación.", HttpStatus.CONFLICT);
        }

        // Validar que no haya duplicidad de vínculo entre usuario y contrato
        if (usuarioContratoRepository.existsByUsuarioAndContrato_Id(usuario, dto.contratoId())
                && !usuarioContrato.getContrato().getId().equals(dto.contratoId())) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.UPDATE_VINCULO_CONTRATO_FALLIDO, Resultado.FALLO, sitio, ip, "3");
            throw new UsuarioContratoCreationException("El usuario ya tiene asignado este contrato.", HttpStatus.CONFLICT);
        }

        usuarioContrato.setUsuario(usuario);
        usuarioContrato.setContrato(contrato);
        usuarioContrato.setNumeroContrato(dto.numeroContrato());
        usuarioContrato.setEstado(dto.status());
        usuarioContrato.setObservaciones(dto.observaciones());

        try {
            usuarioContratoRepository.save(usuarioContrato);
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.UPDATE_VINCULO_CONTRATO_EXITOSO, Resultado.EXITO, sitio, ip, usuarioContrato.getId().toString());
        } catch (DataIntegrityViolationException e) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.UPDATE_VINCULO_CONTRATO_ERROR, Resultado.FALLO, sitio, ip, e.getMostSpecificCause().getMessage());
            throw new UsuarioContratoCreationException("Violación de integridad: " + e.getMostSpecificCause().getMessage(), HttpStatus.CONFLICT);
        } catch (TransactionSystemException e) {
            Throwable root = e.getRootCause();
            String msg = (root instanceof ConstraintViolationException)
                    ? "Violación de restricción: " + root.getMessage()
                    : "Error en la transacción de base de datos";
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.UPDATE_VINCULO_CONTRATO_ERROR, Resultado.FALLO, sitio, ip, msg);
            throw new UsuarioContratoCreationException(msg, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            systemLogService.registrarLogUsuario(usuarioAuth, Evento.UPDATE_VINCULO_CONTRATO_ERROR, Resultado.FALLO, sitio, ip, e.getMessage());
            throw new UsuarioContratoCreationException("Error al actualizar la vinculación: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // --- Eliminar vínculo ---
    @Transactional
    public void eliminarContrato(Long id) throws Exception {
        UsuarioContrato uc = usuarioContratoRepository.findById(id)
                .orElseThrow(() -> new Exception("Contrato de usuario no encontrado"));
        usuarioContratoRepository.delete(uc);
    }
}
