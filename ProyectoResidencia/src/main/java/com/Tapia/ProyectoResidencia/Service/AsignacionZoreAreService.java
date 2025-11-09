package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.AsignacionZoreAreCreate;
import com.Tapia.ProyectoResidencia.DTO.AsignacionZoreAreResponse;
import com.Tapia.ProyectoResidencia.Enum.*;
import com.Tapia.ProyectoResidencia.Model.*;
import com.Tapia.ProyectoResidencia.Repository.*;
import com.Tapia.ProyectoResidencia.Utils.AuthUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AsignacionZoreAreService {

    private final AsignacionZoreAreRepository asignacionRepository;
    private final ZoreRepository zoreRepository;
    private final AreRepository areRepository;
    private final UsuarioService usuarioService;
    private final SystemLogService systemLogService;

    // 🔹 Listar con paginación
    public Page<AsignacionZoreAreResponse> listarPaginadas(Pageable pageable) {
        Page<AsignacionZoreAre> asignaciones = asignacionRepository.findAll(pageable);
        return asignaciones.map(AsignacionZoreAreResponse::new);
    }

    // 🔹 Crear asignación
    @Transactional
    public AsignacionZoreAre crear(Authentication authentication, AsignacionZoreAreCreate dto, Sitio sitio, String ip) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario admin = usuarioService.getUsuarioEntityByCorreo(correo);

        // 1️⃣ Validar existencia de Zore
        Zore zore = zoreRepository.findById(dto.zoreId())
                .orElseThrow(() -> new NoSuchElementException("No existe una ZORE con ID: " + dto.zoreId()));

        // 2️⃣ Validar existencia de Are
        Are are = areRepository.findById(dto.areId())
                .orElseThrow(() -> new NoSuchElementException("No existe una ARE con ID: " + dto.areId()));

        validarAsignacion(zore, are, dto.anio(), admin, sitio, ip, null);

        // ✅ Si pasa todas las validaciones, crear la asignación
        AsignacionZoreAre asignacion = new AsignacionZoreAre();
        asignacion.setAnio(dto.anio());
        asignacion.setZore(zore);
        asignacion.setAre(are);

        return guardarAsignacion(asignacion, Evento.ASIGNACION_ZORE_ARE_REGISTRO_EXITOSO, admin, sitio, ip);
    }

    // 🔹 Actualizar asignación existente
    @Transactional
    public AsignacionZoreAre actualizar(Authentication authentication, Long id, AsignacionZoreAreCreate dto, Sitio sitio, String ip) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario admin = usuarioService.getUsuarioEntityByCorreo(correo);

        AsignacionZoreAre asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Asignación no encontrada con ID: " + id));

        Zore zore = zoreRepository.findById(dto.zoreId())
                .orElseThrow(() -> new NoSuchElementException("No existe una ZORE con ID: " + dto.zoreId()));

        Are are = areRepository.findById(dto.areId())
                .orElseThrow(() -> new NoSuchElementException("No existe una ARE con ID: " + dto.areId()));

        validarAsignacion(zore, are, dto.anio(), admin, sitio, ip, asignacion);

        // ✅ Si pasa todo, actualizar
        asignacion.setAnio(dto.anio());
        asignacion.setZore(zore);
        asignacion.setAre(are);

        return guardarAsignacion(asignacion, Evento.ASIGNACION_ZORE_ARE_UPDATE_EXITOSA, admin, sitio, ip);
    }

    // 🔹 Método genérico para guardar y registrar logs
    private AsignacionZoreAre guardarAsignacion(AsignacionZoreAre asignacion, Evento evento, Usuario admin, Sitio sitio, String ip) {
        Evento eventoError = switch (evento) {
            case ASIGNACION_ZORE_ARE_REGISTRO_EXITOSO -> Evento.ASIGNACION_ZORE_ARE_REGISTRO_ERROR;
            case ASIGNACION_ZORE_ARE_UPDATE_EXITOSA -> Evento.ASIGNACION_ZORE_ARE_UPDATE_ERROR;
            default -> Evento.DESCONOCIDO;
        };

        try {
            AsignacionZoreAre saved = asignacionRepository.save(asignacion);

            String detalle = String.format("Asignación ZORE %d ↔ ARE %d (%s)",
                    asignacion.getZore().getNumeracion(),
                    asignacion.getAre().getNumeracion(),
                    asignacion.getAnio());

            systemLogService.registrarLogUsuario(admin, evento, Resultado.EXITO, sitio, ip, detalle);
            return saved;

        } catch (DataIntegrityViolationException e) {
            String msg = "Violación de integridad en la base de datos: " + e.getMostSpecificCause().getMessage();
            systemLogService.registrarLogUsuario(admin, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (OptimisticLockingFailureException e) {
            String msg = "Conflicto de concurrencia al actualizar registro de Asignación Zore-Are.";
            systemLogService.registrarLogUsuario(admin, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (TransactionSystemException e) {
            Throwable root = e.getRootCause();
            String msg = (root instanceof ConstraintViolationException)
                    ? "Violación de restricción de validación: " + root.getMessage()
                    : "Error en la transacción de la base de datos.";
            systemLogService.registrarLogUsuario(admin, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (Exception e) {
            String msg = "Error inesperado al guardar o actualizar Asignación Zore-Are: " + e.getMessage();
            systemLogService.registrarLogUsuario(admin, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;
        }
    }

    private void validarAsignacion(Zore zore, Are are, String anio, Usuario admin, Sitio sitio, String ip, AsignacionZoreAre asignacionExistente) {
        // Coincidencia de año
        if (!zore.getAnio().equals(are.getAnio())) {
            registrarFallo(admin, sitio, ip, asignacionExistente, "El año de la ZORE y ARE no pertenecen al mismo año");
            throw new IllegalArgumentException(String.format(
                    "El año de la ZORE (%s) y el de la ARE (%s) no coinciden.", zore.getAnio(), are.getAnio()
            ));
        }

        // Duplicado ZORE–ARE
        boolean duplicado = asignacionExistente == null ?
                asignacionRepository.existsByZoreIdAndAreId(zore.getId(), are.getId()) :
                asignacionRepository.existsByZoreIdAndAreIdAndIdNot(zore.getId(), are.getId(), asignacionExistente.getId());

        if (duplicado) {
            registrarFallo(admin, sitio, ip, asignacionExistente, "La ARE " + are.getNumeracion() + " ya está asignada a la ZORE " + zore.getNumeracion() + " para el año " + anio);
            throw new IllegalArgumentException(String.format(
                    "Ya existe una asignación entre la ZORE %d y la ARE %d en el año %s.",
                    zore.getNumeracion(), are.getNumeracion(), anio
            ));
        }

        // ARE ya asignada a otra ZORE
        boolean areAsignada = asignacionRepository.existsByAreIdAndAnio(are.getId(), anio) &&
                (asignacionExistente == null || !asignacionExistente.getAre().getId().equals(are.getId()));

        if (areAsignada) {
            registrarFallo(admin, sitio, ip, asignacionExistente, "La ARE " + are.getNumeracion() + " ya está asignada a otra ZORE en el año " + anio);
            throw new IllegalArgumentException(String.format(
                    "La ARE %d ya está asignada a otra ZORE en el año %s.",
                    are.getNumeracion(), anio
            ));
        }
    }

    private void registrarFallo(Usuario admin, Sitio sitio, String ip, AsignacionZoreAre asignacionExistente, String mensaje) {
        Evento evento = asignacionExistente == null ?
                Evento.ASIGNACION_ZORE_ARE_REGISTRO_FALLIDO : Evento.ASIGNACION_ZORE_ARE_UPDATE_FALLIDO;
        systemLogService.registrarLogUsuario(admin, evento, Resultado.FALLO, sitio, ip, mensaje);
    }
}
