package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.*;
import com.Tapia.ProyectoResidencia.Model.Municipio;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.MunicipioRepository;
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
public class MunicipioService {

    private final MunicipioRepository municipioRepository;
    private final UsuarioService usuarioService;
    private final SystemLogService systemLogService;

    // ✅ Listar todos los municipios
    public List<Municipio> listarTodos() {
        return municipioRepository.findAll();
    }

    // ✅ Crear municipio
    @Transactional
    public Municipio crear(Authentication authentication, Municipio dto, Sitio sitio, String ip) {
        Usuario usuario = obtenerUsuario(authentication);

        // 🔍 Prepara y valída datos del municipio
        Municipio municipio = prepararMunicipio(dto, null, usuario, Evento.MUNICIPIO_REGISTER_FALLIDO, sitio, ip);

        // 🚫 Verificar duplicado de ID
        if (municipioRepository.existsById(municipio.getId())) {
            String msg = "El ID del municipio ya existe: " + municipio.getId();
            systemLogService.registrarLogUsuario(usuario, Evento.MUNICIPIO_REGISTER_FALLIDO, Resultado.FALLO, sitio, ip, msg);
            throw new IllegalArgumentException(msg);
        }

        return guardarMunicipio(municipio, Evento.MUNICIPIO_REGISTER_EXITOSO, usuario, sitio, ip);
    }

    // ✅ Actualizar municipio
    @Transactional
    public Municipio actualizar(Authentication authentication, String id, Municipio dto, Sitio sitio, String ip) {
        Usuario usuario = obtenerUsuario(authentication);

        Municipio municipioExistente = municipioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Municipio no encontrado con ID: " + id));

        Municipio municipioValidado = prepararMunicipio(dto, id, usuario, Evento.MUNICIPIO_UPDATE_FALLIDO, sitio, ip);

        // Si cambió el ID, crear una nueva instancia con el nuevo ID
        if (!municipioValidado.getId().equals(id)) {
            if (municipioRepository.existsById(municipioValidado.getId())) {
                String msg = "El nuevo ID del municipio ya existe: " + municipioValidado.getId();
                systemLogService.registrarLogUsuario(usuario, Evento.MUNICIPIO_UPDATE_FALLIDO, Resultado.FALLO, sitio, ip, msg);
                throw new IllegalArgumentException(msg);
            }

            // Crear nuevo registro con los datos actualizados
            Municipio nuevo = new Municipio();
            nuevo.setId(municipioValidado.getId());
            nuevo.setNombre(municipioValidado.getNombre());
            Municipio guardado = municipioRepository.save(nuevo);

            // Eliminar el registro anterior
            municipioRepository.deleteById(id);

            systemLogService.registrarLogUsuario(usuario, Evento.MUNICIPIO_UPDATE_EXITOSO, Resultado.EXITO, sitio, ip,
                    "Id anterior: " + id + " Nuevo Id:  " + nuevo.getId() + " Nombre: " + nuevo.getNombre());
            return guardado;
        }

        // Si no cambió el ID, solo actualizar el nombre
        municipioExistente.setNombre(municipioValidado.getNombre());
        return guardarMunicipio(municipioExistente, Evento.MUNICIPIO_UPDATE_EXITOSO, usuario, sitio, ip);
    }

    // ✅ Eliminar municipio
    @Transactional
    public void eliminar(Authentication authentication, String id, Sitio sitio, String ip) {
        Usuario usuario = obtenerUsuario(authentication);

        Municipio municipio = municipioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Municipio no encontrado"));

        Evento eventoExito = Evento.MUNICIPIO_DELETE_EXITOSO;
        Evento eventoError = Evento.MUNICIPIO_DELETE_ERROR;

        try {
            String nombre = municipio.getNombre();
            municipioRepository.delete(municipio);
            systemLogService.registrarLogUsuario(usuario, eventoExito, Resultado.EXITO, sitio, ip, id + " - " + nombre);

        } catch (DataIntegrityViolationException e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError, "No se puede eliminar el municipio porque está referenciado en otros registros.");
            throw e;

        } catch (OptimisticLockingFailureException e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError, "Conflicto de concurrencia al eliminar municipio.");
            throw e;

        } catch (TransactionSystemException e) {
            String msg = (e.getRootCause() instanceof ConstraintViolationException)
                    ? "Violación de restricción al eliminar municipio: " + e.getRootCause().getMessage()
                    : "Error en la transacción al eliminar municipio.";
            manejarExcepcionBD(usuario, sitio, ip, eventoError, msg);
            throw e;

        } catch (Exception e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError, "Error inesperado al eliminar municipio: " + e.getMessage());
            throw e;
        }
    }

    // ✅ Prepara y valida un municipio para crear o actualizar
    private Municipio prepararMunicipio(Municipio dto, String idExistente, Usuario usuario, Evento eventoError, Sitio sitio, String ip) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del municipio no puede estar vacío");
        }

        validarDuplicado(dto.getNombre(), idExistente, usuario, eventoError, sitio, ip);

        String idNormalizado = normalizarYValidarId(dto.getId());

        Municipio municipio = new Municipio();
        municipio.setId(idNormalizado);
        municipio.setNombre(dto.getNombre());

        return municipio;
    }

    // ✅ Guardar / actualizar municipio con manejo de errores
    private Municipio guardarMunicipio(Municipio municipio, Evento evento, Usuario usuario, Sitio sitio, String ip) {
        Evento eventoError = switch (evento) {
            case MUNICIPIO_REGISTER_EXITOSO -> Evento.MUNICIPIO_REGISTER_ERROR;
            case MUNICIPIO_UPDATE_EXITOSO -> Evento.MUNICIPIO_UPDATE_ERROR;
            case MUNICIPIO_DELETE_EXITOSO -> Evento.MUNICIPIO_DELETE_ERROR;
            default -> Evento.DESCONOCIDO;
        };

        try {
            Municipio m = municipioRepository.save(municipio);
            systemLogService.registrarLogUsuario(usuario, evento, Resultado.EXITO, sitio, ip, m.getId() + " - " + m.getNombre());
            return m;

        } catch (Exception e) {
            manejarExcepcionBD(usuario, sitio, ip, eventoError, "Error al guardar municipio: " + e.getMessage());
            throw e;
        }
    }

    // ✅ Obtener usuario autenticado
    private Usuario obtenerUsuario(Authentication authentication) {
        String correo = AuthUtils.extractEmailFromAuth(authentication);
        return usuarioService.getUsuarioEntityByCorreo(correo);
    }

    // ✅ Validar duplicado de nombre
    private void validarDuplicado(String nombre, String id, Usuario usuario, Evento evento, Sitio sitio, String ip) {
        boolean duplicado = (id == null)
                ? municipioRepository.existsByNombre(nombre)
                : municipioRepository.existsByNombreAndIdNot(nombre, id);

        if (duplicado) {
            String msg = "El nombre del municipio ya existe: " + nombre;
            systemLogService.registrarLogUsuario(usuario, evento, Resultado.FALLO, sitio, ip, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    // ✅ Normalizar y validar ID
    private String normalizarYValidarId(String idIngresado) {
        if (idIngresado == null || idIngresado.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del municipio no puede estar vacío");
        }

        idIngresado = idIngresado.trim();
        if (!idIngresado.matches("\\d+")) {
            throw new IllegalArgumentException("El ID del municipio solo puede contener números");
        }

        if (idIngresado.length() > 3) {
            throw new IllegalArgumentException("El ID del municipio no puede tener más de 3 dígitos");
        }

        return String.format("%03d", Integer.parseInt(idIngresado));
    }

    // ✅ Manejo unificado de excepciones de base de datos
    private void manejarExcepcionBD(Usuario usuario, Sitio sitio, String ip, Evento eventoError, String mensaje) {
        systemLogService.registrarLogUsuario(usuario, eventoError, Resultado.FALLO, sitio, ip, mensaje);
    }
}
