package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.IdAnioDTO;
import com.Tapia.ProyectoResidencia.DTO.ZoreCreate;
import com.Tapia.ProyectoResidencia.DTO.ZoreResponse;
import com.Tapia.ProyectoResidencia.Enum.*;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.Zore;
import com.Tapia.ProyectoResidencia.Repository.ZoreRepository;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class ZoreService {

    private final ZoreRepository zoreRepository;
    private final SystemLogService systemLogService;
    private final UsuarioService usuarioService;

    public Page<ZoreResponse> listarPaginadas(Pageable pageable) {
        Page<Zore> zores = zoreRepository.findAll(pageable);
        return zores.map(ZoreResponse::new);
    }

    @Transactional
    public Zore crear(Authentication authentication, ZoreCreate dto, Sitio sitio, String ip) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario admin = usuarioService.getUsuarioEntityByCorreo(correo);

        Usuario usuarioAsignado = validarZore(dto, null, admin, Evento.ZORE_REGISTER_FALLIDO, sitio, ip);

        Zore zore = new Zore();
        zore.setNumeracion(dto.numeracion());
        zore.setAnio(dto.anio());
        zore.setUsuario(usuarioAsignado);

        return guardarZore(zore, Evento.ZORE_REGISTER_EXITOSO, admin, sitio, ip);
    }

    @Transactional
    public Zore actualizar(Authentication authentication, Long id, ZoreCreate dto, Sitio sitio, String ip) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        Usuario admin = usuarioService.getUsuarioEntityByCorreo(correo);

        Zore zore = zoreRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Zore no encontrada con ID: " + id));

        Usuario usuarioAsignado = validarZore(dto, id, admin, Evento.ZORE_UPDATE_FALLIDO, sitio, ip);

        zore.setNumeracion(dto.numeracion());
        zore.setAnio(dto.anio());
        zore.setUsuario(usuarioAsignado);

        return guardarZore(zore, Evento.ZORE_UPDATE_EXITOSO, admin, sitio, ip);
    }

    // Devuelve lista de años únicos
    public List<IdAnioDTO> obtenerAniosUnicos() {
        List<Zore> todas = zoreRepository.findAll();
        Map<String, Zore> map = new LinkedHashMap<>();
        for (Zore z : todas) {
            map.putIfAbsent(z.getAnio(), z); // solo el primero de cada año
        }
        return map.values().stream()
                .map(z -> new IdAnioDTO(z.getId(), z.getAnio()))
                .toList();
    }

    // Devuelve todas las zores de un año
    public List<ZoreResponse> listarPorAnio(String anio) {
        return zoreRepository.findByAnio(anio).stream()
                .map(ZoreResponse::new)
                .toList();
    }

    private Zore guardarZore(Zore zore, Evento evento, Usuario user, Sitio sitio, String ip) {
        Evento eventoError = switch (evento) {
            case ZORE_REGISTER_EXITOSO -> Evento.ZORE_REGISTER_ERROR;
            case ZORE_UPDATE_EXITOSO -> Evento.ZORE_UPDATE_ERROR;
            default -> Evento.DESCONOCIDO;
        };

        try {
            Zore saved = zoreRepository.save(zore);
            String detalle = String.format("Zore %s (%s) asignada a %s",
                    zore.getNumeracion(), zore.getAnio(),
                    zore.getUsuario().getCorreo());
            systemLogService.registrarLogUsuario(user, evento, Resultado.EXITO, sitio, ip, detalle);
            return saved;
        } catch (DataIntegrityViolationException e) {
            String msg = "Violación de integridad en la BD: " + e.getMostSpecificCause().getMessage();
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;

        } catch (OptimisticLockingFailureException e) {
            String msg = "Conflicto de concurrencia al actualizar registro de Zore.";
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
            String msg = "Error inesperado al guardar o actualizar Zore: " + e.getMessage();
            systemLogService.registrarLogUsuario(user, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw e;
        }
    }

    private void validarDuplicado(int numeracion, String anio, Long id, Usuario usuario, Evento evento, Sitio sitio, String ip) {
        boolean duplicado = (id == null)
                ? zoreRepository.existsByNumeracionAndAnio(numeracion, anio)
                : zoreRepository.existsByNumeracionAndAnioAndIdNot(numeracion, anio, id);

        if (duplicado) {
            systemLogService.registrarLogUsuario(usuario, evento, Resultado.FALLO, sitio, ip,
                    "Ya existe un registro con la misma numeración y año");
            throw new IllegalArgumentException("Ya existe una Zore con esa numeración y año");
        }
    }

    private Usuario validarZore(ZoreCreate dto, Long id, Usuario admin, Evento eventoFallo, Sitio sitio, String ip) {
        // === 1️⃣ Validar usuario asignado ===
        Usuario usuarioAsignado = usuarioService.getUsuarioById(dto.usuarioId());
        if (usuarioAsignado == null) {
            systemLogService.registrarLogUsuario(admin, eventoFallo, Resultado.FALLO, sitio, ip,
                    "Intento de asignar una Zore a un usuario inexistente (ID: " + dto.usuarioId() + ")");
            throw new IllegalArgumentException("El usuario asignado no existe.");
        }

        if (usuarioAsignado.getRol() != Rol.SE) {
            systemLogService.registrarLogUsuario(admin, eventoFallo, Resultado.FALLO, sitio, ip,
                    "Intento de asignar una Zore a un usuario con rol inválido (" + usuarioAsignado.getRol() + ")");
            throw new IllegalArgumentException("Solo los usuarios con rol SE pueden ser asignados a una Zore.");
        }

        // === 2️⃣ Validar numeración duplicada ===
        validarDuplicado(dto.numeracion(), dto.anio(), id, admin, eventoFallo, sitio, ip);

        // === 3️⃣ Validar si el usuario ya tiene Zore en el mismo año (excluyendo la actual si aplica) ===
        Optional<Zore> existentePorUsuario = zoreRepository.findByUsuarioAndAnio(usuarioAsignado, dto.anio());
        if (existentePorUsuario.isPresent() && (id == null || !existentePorUsuario.get().getId().equals(id))) {
            systemLogService.registrarLogUsuario(admin, eventoFallo, Resultado.FALLO, sitio, ip,
                    "Intento de asignar al usuario " + usuarioAsignado.getNombre() + " (" + usuarioAsignado.getCorreo() +
                            ") otra Zore en el año " + dto.anio());
            throw new IllegalArgumentException("El usuario ya tiene asignada una Zore en el año " + dto.anio());
        }

        // === 4️⃣ Validar formato y rango del año ===
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
