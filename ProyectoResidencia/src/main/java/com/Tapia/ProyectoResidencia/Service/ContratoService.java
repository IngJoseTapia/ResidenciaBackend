package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.ContratoCreate;
import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ContratoCreationException;
import com.Tapia.ProyectoResidencia.Model.Contrato;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.ContratoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final SystemLogService systemLogService;
    private final UsuarioService usuarioService;

    // Obtener todos los contratos
    public Page<Contrato> obtenerContratosPaginado(Pageable pageable) {
        return contratoRepository.findAll(pageable);
    }

    // Crear un nuevo contrato
    @Transactional
    public Contrato crearContrato(ContratoCreate dto, Authentication auth, Sitio sitio, String ip) {
        // Obtener usuario autenticado
        String correoAuth = auth.getName();
        Usuario usuarioAuth = usuarioService.getUsuarioEntityByCorreo(correoAuth);

        // Mapear DTO a entidad
        Contrato contrato = new Contrato();
        contrato.setPuesto(dto.puesto());
        contrato.setCodigo(dto.codigo());
        contrato.setNivelTabular(dto.nivelTabular());
        contrato.setFechaInicio(dto.fechaInicio());
        contrato.setFechaConclusion(dto.fechaConclusion());
        contrato.setActividadesGenericas(dto.actividadesGenericas());
        contrato.setSueldo(dto.sueldo());

        // Validación: código duplicado
        validarDuplicado(dto.codigo(), contrato.getId(), usuarioAuth, Evento.CREATE_CONTRATO_FALLIDO, sitio, ip);

        // Validación: fecha de conclusión antes de inicio
        validarFechas(contrato, Evento.CREATE_CONTRATO_FALLIDO, usuarioAuth, sitio, ip);

        // Validación: sueldo nulo o negativo
        validarSueldo(contrato, Evento.CREATE_CONTRATO_FALLIDO, usuarioAuth, sitio, ip);

        return guardarContrato(contrato, Evento.CREATE_CONTRATO_EXITOSO, usuarioAuth, sitio, ip);
    }

    // Buscar contrato por ID
    public Contrato obtenerPorId(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado con ID: " + id));
    }

    // Actualizar contrato existente
    @Transactional
    public Contrato actualizar(Authentication auth, Long id, ContratoCreate dto, Sitio sitio, String ip) {
        String correo = auth.getName();
        Usuario user = usuarioService.getUsuarioEntityByCorreo(correo);

        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        // Validar duplicado (ignorando este id)
        validarDuplicado(dto.codigo(), id, user, Evento.UPDATE_CONTRATO_FALLIDO, sitio, ip);

        contrato.setPuesto(dto.puesto());
        contrato.setCodigo(dto.codigo());
        contrato.setNivelTabular(dto.nivelTabular());
        contrato.setFechaInicio(dto.fechaInicio());
        contrato.setFechaConclusion(dto.fechaConclusion());
        contrato.setActividadesGenericas(dto.actividadesGenericas());
        contrato.setSueldo(dto.sueldo());

        // Validación: fecha de conclusión antes de inicio
        validarFechas(contrato, Evento.UPDATE_CONTRATO_FALLIDO, user, sitio, ip);

        // Validación: sueldo nulo o negativo
        validarSueldo(contrato, Evento.UPDATE_CONTRATO_FALLIDO, user, sitio, ip);

        return guardarContrato(contrato, Evento.UPDATE_CONTRATO_EXITOSO, user, sitio, ip);
    }

    // Eliminar contrato por ID
    @Transactional
    public void eliminar(Authentication auth, Long id, Sitio sitio, String ip) {
        String correo = auth.getName();
        Usuario user = usuarioService.getUsuarioEntityByCorreo(correo);

        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));

        try {
            String idContrato = contrato.getPuesto();
            contratoRepository.delete(contrato);
            systemLogService.registrarLogUsuario(user, Evento.DELETE_CONTRATO_EXITOSO, Resultado.EXITO, sitio, ip, idContrato);
        } catch (Exception e) {
            systemLogService.registrarLogUsuario(user, Evento.DELETE_CONTRATO_ERROR, Resultado.FALLO, sitio, ip,
                    "Error al eliminar contrato: " + e.getMessage());
            throw e;
        }
    }

    private Contrato guardarContrato(Contrato contrato, Evento eventoExito, Usuario user, Sitio sitio, String ip) {
        Evento eventoError = switch (eventoExito) {
            case CREATE_CONTRATO_EXITOSO -> Evento.CREATE_CONTRATO_ERROR;
            case UPDATE_CONTRATO_EXITOSO -> Evento.UPDATE_CONTRATO_ERROR;
            default -> Evento.DESCONOCIDO;
        };

        try {
            Contrato saved = contratoRepository.save(contrato);
            systemLogService.registrarLogUsuario(user, eventoExito, Resultado.EXITO, sitio, ip, contrato.getPuesto());
            return saved;
        } catch (DataIntegrityViolationException e) {
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip,
                    "Violación de integridad en la BD: " + e.getMostSpecificCause().getMessage());
            throw e;
        } catch (TransactionSystemException e) {
            Throwable root = e.getRootCause();
            String msg = (root instanceof ConstraintViolationException)
                    ? "Error de validación: " + root.getMessage()
                    : "Error en la transacción de la base de datos";
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;
        } catch (Exception e) {
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip,
                    "Error inesperado: " + e.getMessage());
            throw e;
        }
    }

    private void validarDuplicado(String codigo, Long id, Usuario user, Evento evento, Sitio sitio, String ip) {
        boolean duplicado = (id == null)
                ? contratoRepository.existsByCodigo(codigo)
                : contratoRepository.existsByCodigoAndIdNot(codigo, id);

        if (duplicado) {
            systemLogService.registrarLogUsuario(user, evento, Resultado.FALLO, sitio, ip, "1");
            throw new ContratoCreationException("Ya existe un contrato con el código: " + codigo, HttpStatus.CONFLICT);
        }
    }

    private void validarFechas(Contrato contrato, Evento evento, Usuario user, Sitio sitio, String ip) {
        if (contrato.getFechaConclusion().isBefore(contrato.getFechaInicio())) {
            systemLogService.registrarLogUsuario(user, evento, Resultado.FALLO, sitio, ip, "2");
            throw new ContratoCreationException("La fecha de conclusión no puede ser anterior a la de inicio", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarSueldo(Contrato contrato, Evento evento, Usuario user, Sitio sitio, String ip) {
        if (contrato.getSueldo() == null || contrato.getSueldo().doubleValue() <= 0) {
            systemLogService.registrarLogUsuario(user, evento, Resultado.FALLO, sitio, ip, "3");
            throw new ContratoCreationException("El sueldo debe ser mayor que 0", HttpStatus.BAD_REQUEST);
        }
    }
}
