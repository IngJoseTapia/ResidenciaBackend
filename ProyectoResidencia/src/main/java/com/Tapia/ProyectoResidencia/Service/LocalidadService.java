package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.LocalidadCreate;
import com.Tapia.ProyectoResidencia.DTO.LocalidadResponse;
import com.Tapia.ProyectoResidencia.Enum.*;
import com.Tapia.ProyectoResidencia.Model.Localidad;
import com.Tapia.ProyectoResidencia.Model.Municipio;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.LocalidadRepository;
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

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class LocalidadService {

    private final LocalidadRepository localidadRepository;
    private final UsuarioService usuarioService;
    private final SystemLogService systemLogService;
    private final MunicipioService municipioService;

    // ✅ Listar todas las localidades
    public List<Localidad> listarTodas() {
        return localidadRepository.findAll();
    }

    public Page<LocalidadResponse> listarPaginadas(Pageable pageable) {
        return localidadRepository.findAll(pageable)
                .map(LocalidadResponse::new);
    }

    // ✅ Crear localidad
    @Transactional
    public Localidad crear(Authentication authentication, LocalidadCreate dto, Sitio sitio, String ip) {
        Usuario usuario = obtenerUsuario(authentication);

        System.out.println("id municipio: " + dto.municipioId());
        Municipio municipio = municipioService.obtenerPorId(dto.municipioId())
                .orElseThrow(() -> new NoSuchElementException("Municipio no encontrado con ID: " + dto.municipioId()));

        validarCampos(dto, null, municipio, usuario, Evento.LOCALIDAD_REGISTER_FALLIDO, sitio, ip);

        Localidad nueva = new Localidad();
        nueva.setNumeroLocalidad(dto.numeroLocalidad());
        nueva.setNombre(dto.nombre());
        nueva.setMunicipio(municipio);

        try {
            Localidad guardada = localidadRepository.save(nueva);
            systemLogService.registrarLogUsuario(usuario, Evento.LOCALIDAD_REGISTER_EXITOSO, Resultado.EXITO, sitio, ip,
                    "ID: " + guardada.getId() + " - " + guardada.getNombre());
            return guardada;

        } catch (Exception e) {
            manejarExcepcionBD(usuario, sitio, ip, Evento.LOCALIDAD_REGISTER_ERROR, "Error al crear localidad: " + e.getMessage());
            throw e;
        }
    }

    // ✅ Actualizar localidad
    @Transactional
    public Localidad actualizar(Authentication authentication, Long id, LocalidadCreate dto, Sitio sitio, String ip) {
        Usuario usuario = obtenerUsuario(authentication);

        Localidad existente = localidadRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Localidad no encontrada con ID: " + id));

        Municipio municipio = municipioService.obtenerPorId(dto.municipioId())
                .orElseThrow(() -> new NoSuchElementException("Municipio no encontrado con ID: " + dto.municipioId()));

        validarCampos(dto, id, municipio, usuario, Evento.LOCALIDAD_UPDATE_FALLIDO, sitio, ip);

        existente.setNumeroLocalidad(dto.numeroLocalidad());
        existente.setNombre(dto.nombre());

        try {
            existente.setMunicipio(municipio);
            Localidad actualizada = localidadRepository.save(existente);
            systemLogService.registrarLogUsuario(usuario, Evento.LOCALIDAD_UPDATE_EXITOSO, Resultado.EXITO, sitio, ip,
                    "ID: " + id + " - " + actualizada.getNombre());
            return actualizada;

        } catch (Exception e) {
            manejarExcepcionBD(usuario, sitio, ip, Evento.LOCALIDAD_UPDATE_ERROR, "Error al actualizar localidad: " + e.getMessage());
            throw e;
        }
    }

    // ✅ Eliminar localidad
    @Transactional
    public void eliminar(Authentication authentication, Long id, Sitio sitio, String ip) {
        Usuario usuario = obtenerUsuario(authentication);

        Localidad localidad = localidadRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Localidad no encontrada"));

        Evento eventoExito = Evento.LOCALIDAD_DELETE_EXITOSO;
        Evento eventoError = Evento.LOCALIDAD_DELETE_ERROR;

        try {
            localidadRepository.delete(localidad);
            systemLogService.registrarLogUsuario(usuario, eventoExito, Resultado.EXITO, sitio, ip,
                    "ID: " + id + " - " + localidad.getNombre());

        } catch (DataIntegrityViolationException e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError, "No se puede eliminar la localidad porque está referenciada en otros registros.");
            throw e;

        } catch (OptimisticLockingFailureException e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError, "Conflicto de concurrencia al eliminar localidad.");
            throw e;

        } catch (TransactionSystemException e) {
            String msg = (e.getRootCause() instanceof ConstraintViolationException)
                    ? "Violación de restricción al eliminar localidad: " + e.getRootCause().getMessage()
                    : "Error en la transacción al eliminar localidad.";
            manejarExcepcionBD(usuario, sitio, ip, eventoError, msg);
            throw e;

        } catch (Exception e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError, "Error inesperado al eliminar localidad: " + e.getMessage());
            throw e;
        }
    }

    private void validarCampos(LocalidadCreate dto, Long idExistente, Municipio municipio,
                               Usuario usuario, Evento eventoError, Sitio sitio, String ip) {

        if (dto.numeroLocalidad() == null || dto.numeroLocalidad().trim().isEmpty())
            throw new IllegalArgumentException("El número de localidad no puede estar vacío");

        if (dto.nombre() == null || dto.nombre().trim().isEmpty())
            throw new IllegalArgumentException("El nombre de la localidad no puede estar vacío");

        boolean duplicadoNumero = (idExistente == null)
                ? localidadRepository.existsByMunicipioAndNumeroLocalidad(municipio, dto.numeroLocalidad())
                : localidadRepository.existsByMunicipioAndNumeroLocalidadAndIdNot(municipio, dto.numeroLocalidad(), idExistente);

        if (duplicadoNumero) {
            String msg = "Ya existe una localidad con el número " + dto.numeroLocalidad() +
                    " en el municipio " + municipio.getNombre();
            systemLogService.registrarLogUsuario(usuario, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw new IllegalArgumentException(msg);
        }

        boolean duplicadoNombre = (idExistente == null)
                ? localidadRepository.existsByMunicipioAndNombre(municipio, dto.nombre())
                : localidadRepository.existsByMunicipioAndNombreAndIdNot(municipio, dto.nombre(), idExistente);

        if (duplicadoNombre) {
            String msg = "Ya existe una localidad con el nombre '" + dto.nombre() +
                    "' en el municipio " + municipio.getNombre();
            systemLogService.registrarLogUsuario(usuario, eventoError, Resultado.FALLO, sitio, ip, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    // ✅ Obtener usuario autenticado
    private Usuario obtenerUsuario(Authentication authentication) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        return usuarioService.getUsuarioEntityByCorreo(correo);
    }

    // ✅ Manejo unificado de excepciones de base de datos
    private void manejarExcepcionBD(Usuario usuario, Sitio sitio, String ip, Evento eventoError, String mensaje) {
        systemLogService.registrarLogUsuario(usuario, eventoError, Resultado.FALLO, sitio, ip, mensaje);
    }
}
