package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.AreCreate;
import com.Tapia.ProyectoResidencia.DTO.AreResponse;
import com.Tapia.ProyectoResidencia.Enum.*;
import com.Tapia.ProyectoResidencia.Model.Are;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.AreRepository;
import com.Tapia.ProyectoResidencia.Repository.AsignacionZoreAreRepository;
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

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AreService {

    private final AreRepository areRepository;
    private final SystemLogService systemLogService;
    private final UsuarioService usuarioService;
    private final AsignacionZoreAreRepository asignacionZoreAreRepository;

    public Page<AreResponse> listarPaginadas(Pageable pageable) {
        Page<Are> ares = areRepository.findAll(pageable);
        return ares.map(AreResponse::new);
    }

    @Transactional
    public Are crear(Authentication authentication, AreCreate dto, Sitio sitio, String ip) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario admin = usuarioService.getUsuarioEntityByCorreo(correo);

        Usuario usuarioAsignado = validarAre(dto, null, admin, Evento.ARE_REGISTER_FALLIDO, sitio, ip);

        Are are = new Are();
        are.setNumeracion(dto.numeracion());
        are.setAnio(dto.anio());
        are.setUsuario(usuarioAsignado);

        return guardarAre(are, Evento.ARE_REGISTER_EXITOSO, admin, sitio, ip);
    }

    @Transactional
    public Are actualizar(Authentication authentication, Long id, AreCreate dto, Sitio sitio, String ip) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario admin = usuarioService.getUsuarioEntityByCorreo(correo);

        Are are = areRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Are no encontrada con ID: " + id));

        Usuario usuarioAsignado = validarAre(dto, id, admin, Evento.ARE_UPDATE_FALLIDO, sitio, ip);

        are.setNumeracion(dto.numeracion());
        are.setAnio(dto.anio());
        are.setUsuario(usuarioAsignado);

        return guardarAre(are, Evento.ARE_UPDATE_EXITOSO, admin, sitio, ip);
    }

    // Devuelve ARE de un año que no estén asignadas,
    // pero permite incluir explícitamente una ARE (por ID) aunque esté asignada.
    public List<AreResponse> listarPorAnioSinAsignacion(String anio, Long includeId) {
        // 1️⃣ Obtener todas las ARE del año
        List<Are> ares = areRepository.findByAnio(anio);

        // 2️⃣ Obtener los ID de ARE asignadas ese año
        List<Long> asignadas = asignacionZoreAreRepository.findAll().stream()
                .filter(a -> a.getAnio().equals(anio))
                .map(a -> a.getAre().getId())
                .toList();

        // 3️⃣ Filtrar solo las no asignadas → convertir a lista mutable
        List<AreResponse> disponibles = new ArrayList<>(
                ares.stream()
                        .filter(a -> !asignadas.contains(a.getId()))
                        .map(AreResponse::new)
                        .toList()
        );

        // 4️⃣ Si se pasa includeId, añadir esa ARE aunque esté asignada
        if (includeId != null) {
            boolean yaIncluida = disponibles.stream()
                    .anyMatch(a -> a.id().equals(includeId));

            if (!yaIncluida) {
                areRepository.findById(includeId).ifPresent(are -> {
                    if (are.getAnio().equals(anio)) {
                        // la añadimos al inicio
                        disponibles.add(0, new AreResponse(are));
                    }
                });
            }
        }

        return disponibles;
    }

    private Are guardarAre(Are are, Evento evento, Usuario user, Sitio sitio, String ip) {
        Evento eventoError = switch (evento) {
            case ARE_REGISTER_EXITOSO -> Evento.ARE_REGISTER_ERROR;
            case ARE_UPDATE_EXITOSO -> Evento.ARE_UPDATE_ERROR;
            default -> Evento.DESCONOCIDO;
        };

        try {
            Are saved = areRepository.save(are);
            String detalle = String.format("Are %s (%s) asignada a %s",
                    are.getNumeracion(), are.getAnio(),
                    are.getUsuario().getCorreo());
            systemLogService.registrarLogUsuario(user, evento, Resultado.EXITO, sitio, ip, detalle);
            return saved;
        } catch (DataIntegrityViolationException e) {
            String msg = "Violación de integridad en la BD: " + e.getMostSpecificCause().getMessage();
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (OptimisticLockingFailureException e) {
            String msg = "Conflicto de concurrencia al actualizar registro de Are.";
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
            String msg = "Error inesperado al guardar o actualizar Are: " + e.getMessage();
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;
        }
    }

    private void validarDuplicado(int numeracion, String anio, Long id, Usuario usuario, Evento evento, Sitio sitio, String ip) {
        boolean duplicado = (id == null)
                ? areRepository.existsByNumeracionAndAnio(numeracion, anio)
                : areRepository.existsByNumeracionAndAnioAndIdNot(numeracion, anio, id);

        if (duplicado) {
            systemLogService.registrarLogUsuario(usuario, evento, Resultado.FALLO, sitio, ip,
                    "Ya existe un registro con la misma numeración y año");
            throw new IllegalArgumentException("Ya existe una Are con esa numeración y año");
        }
    }

    private Usuario validarAre(AreCreate dto, Long id, Usuario admin, Evento eventoFallo, Sitio sitio, String ip) {
        // 1️⃣ Validar usuario
        Usuario usuarioAsignado = usuarioService.getUsuarioById(dto.usuarioId());
        if (usuarioAsignado == null) {
            systemLogService.registrarLogUsuario(admin, eventoFallo, Resultado.FALLO, sitio, ip,
                    "Intento de asignar una Are a un usuario inexistente (ID: " + dto.usuarioId() + ")");
            throw new IllegalArgumentException("El usuario asignado no existe.");
        }

        if (usuarioAsignado.getRol() != Rol.CAE) {
            systemLogService.registrarLogUsuario(admin, eventoFallo, Resultado.FALLO, sitio, ip,
                    "Intento de asignar una Are a un usuario con rol inválido (" + usuarioAsignado.getRol() + ")");
            throw new IllegalArgumentException("Solo los usuarios con rol CAE pueden ser asignados a una Are.");
        }

        // 2️⃣ Validar duplicado
        validarDuplicado(dto.numeracion(), dto.anio(), id, admin, eventoFallo, sitio, ip);

        // 3️⃣ Validar si el usuario ya tiene una Are en el mismo año
        Optional<Are> existentePorUsuario = areRepository.findByUsuarioAndAnio(usuarioAsignado, dto.anio());
        if (existentePorUsuario.isPresent() && (id == null || !existentePorUsuario.get().getId().equals(id))) {
            systemLogService.registrarLogUsuario(admin, eventoFallo, Resultado.FALLO, sitio, ip,
                    "Intento de asignar al usuario " + usuarioAsignado.getNombre() + " otra Are en el año " + dto.anio());
            throw new IllegalArgumentException("El usuario ya tiene asignada una Are en el año " + dto.anio());
        }

        // 4️⃣ Validar año
        int anio;
        try {
            anio = Integer.parseInt(dto.anio());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El año debe ser numérico.");
        }

        int anioActual = Year.now().getValue();
        if (anio < 2000 || anio > anioActual + 1) {
            throw new IllegalArgumentException("El año debe estar dentro de un rango válido (actual o siguiente).");
        }

        return usuarioAsignado;
    }
}
