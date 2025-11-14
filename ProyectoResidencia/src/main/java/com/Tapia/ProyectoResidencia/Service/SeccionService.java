package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.SeccionCreate;
import com.Tapia.ProyectoResidencia.DTO.SeccionResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeccionService {

    private final SeccionRepository seccionRepository;
    private final AsignacionZoreAreRepository asignacionRepository;
    private final LocalidadRepository localidadRepository;
    private final UsuarioService usuarioService;
    private final SystemLogService systemLogService;

    // 📄 Listar secciones paginadas
    public Page<SeccionResponse> listarPaginadas(Pageable pageable) {
        return seccionRepository.findAll(pageable)
                .map(SeccionResponse::new);
    }

    public List<SeccionResponse> listarTodas() {
        return seccionRepository.findAll().stream()
                .map(SeccionResponse::new)
                .collect(Collectors.toList());
    }

    // 🟢 Crear sección
    @Transactional
    public Seccion crear(Authentication auth, SeccionCreate dto, Sitio sitio, String ip) {
        Usuario usuario = obtenerUsuario(auth);

        AsignacionZoreAre asignacion = asignacionRepository.findById(dto.asignacionZoreAreId())
                .orElseThrow(() -> new NoSuchElementException("No existe la asignación Zore-Are especificada."));

        if (dto.localidadesIds() == null || dto.localidadesIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una localidad");
        }

        validarAnios(dto.anio(), asignacion, usuario, sitio, ip);
        validarDuplicado(dto.numeroSeccion(), dto.anio(), asignacion.getId(), null, usuario, sitio, ip);

        Set<Localidad> localidades = obtenerLocalidades(dto.localidadesIds());
        validarMunicipios(localidades, usuario, sitio, ip);

        Seccion seccion = new Seccion();
        seccion.setNumeroSeccion(dto.numeroSeccion());
        seccion.setAnio(dto.anio());
        seccion.setAsignacionZoreAre(asignacion);
        seccion.setLocalidades(localidades);

        return guardarSeccion(seccion, Evento.SECCION_REGISTER_EXITOSO, usuario, sitio, ip);
    }

    // 🟡 Actualizar sección
    @Transactional
    public Seccion actualizar(Authentication auth, Long id, SeccionCreate dto, Sitio sitio, String ip) {
        Usuario usuario = obtenerUsuario(auth);

        Seccion existente = seccionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la sección con ID: " + id));

        AsignacionZoreAre asignacion = asignacionRepository.findById(dto.asignacionZoreAreId())
                .orElseThrow(() -> new NoSuchElementException("No existe la asignación Zore-Are especificada."));

        if (dto.localidadesIds() == null || dto.localidadesIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una localidad");
        }

        validarAnios(dto.anio(), asignacion, usuario, sitio, ip);
        validarDuplicado(dto.numeroSeccion(), dto.anio(), asignacion.getId(), id, usuario, sitio, ip);

        Set<Localidad> localidades = obtenerLocalidades(dto.localidadesIds());
        validarMunicipios(localidades, usuario, sitio, ip);

        existente.setNumeroSeccion(dto.numeroSeccion());
        existente.setAnio(dto.anio());
        existente.setAsignacionZoreAre(asignacion);
        existente.setLocalidades(localidades);

        return guardarSeccion(existente, Evento.SECCION_UPDATE_EXITOSO, usuario, sitio, ip);
    }

    // ✅ Guardar / actualizar con manejo de errores unificado
    private Seccion guardarSeccion(Seccion seccion, Evento eventoExito, Usuario usuario, Sitio sitio, String ip) {
        Evento eventoError = switch (eventoExito) {
            case SECCION_REGISTER_EXITOSO -> Evento.SECCION_REGISTER_ERROR;
            case SECCION_UPDATE_EXITOSO -> Evento.SECCION_UPDATE_ERROR;
            default -> Evento.DESCONOCIDO;
        };

        try {
            Seccion saved = seccionRepository.save(seccion);
            String detalle = String.format("Sección %s - Año %s", saved.getNumeroSeccion(), saved.getAnio());
            systemLogService.registrarLogUsuario(usuario, eventoExito, Resultado.EXITO, sitio, ip, detalle);
            return saved;

        } catch (DataIntegrityViolationException e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError,
                    "Violación de integridad en BD: " + e.getMostSpecificCause().getMessage());
            throw e;

        } catch (OptimisticLockingFailureException e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError,
                    "Conflicto de concurrencia al guardar sección.");
            throw e;

        } catch (TransactionSystemException e) {
            String msg = (e.getRootCause() instanceof ConstraintViolationException)
                    ? "Violación de restricción de validación: " + e.getRootCause().getMessage()
                    : "Error en la transacción de la base de datos.";
            manejarExcepcionBD(usuario, sitio, ip, eventoError, msg);
            throw e;

        } catch (Exception e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError,
                    "Error inesperado al guardar sección: " + e.getMessage());
            throw e;
        }
    }

    // 🔒 Validaciones
    private void validarAnios(String anio, AsignacionZoreAre asignacion, Usuario usuario, Sitio sitio, String ip) {
        if (!anio.equals(asignacion.getAnio())) {
            String msg = String.format("El año de la sección (%s) no coincide con el de la asignación Zore-Are (%s)",
                    anio, asignacion.getAnio());
            systemLogService.registrarLogUsuario(usuario, Evento.SECCION_REGISTER_FALLIDO, Resultado.FALLO, sitio, ip, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    private void validarDuplicado(String numeroSeccion, String anio, Long asignacionId, Long idExistente, Usuario usuario, Sitio sitio, String ip) {
        boolean existe = (idExistente == null)
                ? seccionRepository.existsByNumeroSeccionAndAnioAndAsignacionZoreAre_Id(numeroSeccion, anio, asignacionId)
                : seccionRepository.existsByNumeroSeccionAndAnioAndAsignacionZoreAre_IdAndIdNot(numeroSeccion, anio, asignacionId, idExistente);
        if (existe) {
            String msg = "Ya existe la sección " + numeroSeccion + " para el año " + anio + " y asignación especificada";
            systemLogService.registrarLogUsuario(usuario, Evento.SECCION_REGISTER_FALLIDO, Resultado.FALLO, sitio, ip, msg);
            throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
        }
    }

    private void validarMunicipios(Set<Localidad> localidades, Usuario usuario, Sitio sitio, String ip) {
        if (localidades.isEmpty())
            throw new IllegalArgumentException("Debe seleccionar al menos una localidad");

        String municipioRef = localidades.iterator().next().getMunicipio().getId();
        boolean municipiosDiferentes = localidades.stream()
                .anyMatch(l -> !l.getMunicipio().getId().equals(municipioRef));

        if (municipiosDiferentes) {
            String msg = "No se pueden asignar localidades de diferentes municipios a una misma sección";
            systemLogService.registrarLogUsuario(usuario, Evento.SECCION_REGISTER_FALLIDO, Resultado.FALLO, sitio, ip, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    private Set<Localidad> obtenerLocalidades(Set<Long> ids) {
        return ids.stream()
                .map(id -> localidadRepository.findById(id)
                        .orElseThrow(() -> new NoSuchElementException("No existe localidad con ID: " + id)))
                .collect(Collectors.toSet());
    }

    private Usuario obtenerUsuario(Authentication auth) {
        String correo = AuthUtils.extractEmailFromAuth(auth);
        return usuarioService.getUsuarioEntityByCorreo(correo);
    }

    private void manejarExcepcionBD(Usuario usuario, Sitio sitio, String ip, Evento eventoError, String mensaje) {
        systemLogService.registrarLogUsuario(usuario, eventoError, Resultado.FALLO, sitio, ip, mensaje);
    }
}
