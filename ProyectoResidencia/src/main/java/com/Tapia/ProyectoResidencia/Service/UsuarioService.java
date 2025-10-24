package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.ChangePasswordRequest;
import com.Tapia.ProyectoResidencia.DTO.UpdateUserRequest;
import com.Tapia.ProyectoResidencia.Enum.*;
import com.Tapia.ProyectoResidencia.Exception.UserNotFoundException;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Usuario registrarUsuario(String correo,
                                 String contrasena,
                                 String nombre,
                                 String aPaterno,
                                 String aMaterno,
                                 String genero,
                                 String telefono) {
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(contrasena));
        usuario.setNombre(nombre);
        usuario.setApellidoPaterno(aPaterno);
        usuario.setApellidoMaterno(aMaterno);
        usuario.setGenero(genero);
        usuario.setTelefono(telefono);
        usuario.setRol(Rol.USER);
        usuario.setStatus(Status.PENDIENTE);
        usuario.setFechaRegistro(Date.from(Instant.now()));
        return createUser(usuario);
    }

    public Usuario registrarUsuarioGoogle(String correo, String nombre) {
        return usuarioRepository.findByCorreo(correo).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setCorreo(correo);
            u.setNombre(nombre != null ? nombre : "Usuario Google");
            u.setApellidoPaterno("N/A");
            u.setApellidoMaterno("N/A");
            u.setGenero("Otro");
            u.setTelefono("0000000000");
            u.setRol(Rol.USER);
            u.setStatus(Status.PENDIENTE);
            u.setFechaRegistro(Date.from(Instant.now()));
            String randomPassword = "AUTO_" + UUID.randomUUID();
            u.setContrasena(randomPassword); // sin encode
            createUser(u);
            return u;
        });
    }

    public Usuario actualizarUsuario(Usuario usuario, UpdateUserRequest request){
        usuario.setNombre(request.nombre());
        usuario.setApellidoPaterno(request.apellidoPaterno());
        usuario.setApellidoMaterno(request.apellidoMaterno());
        usuario.setGenero(request.genero());
        usuario.setTelefono(request.telefono());

        // ✅ Validar antes de guardar
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);

        if (!violations.isEmpty()) {
            String mensajes = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ConstraintViolationException("Errores de validación: " + mensajes, violations);
        }

        return createUser(usuario);
    }

    public void actualizarContrasena(Usuario usuario, ChangePasswordRequest request){
        usuario.setContrasena(passwordEncoder.encode(request.nuevaPassword()));
        createUser(usuario);
    }

    public void restablecerContrasena(Usuario usuario, String password){
        usuario.setContrasena(passwordEncoder.encode(password));
        createUser(usuario);
    }

    public Usuario getUsuarioEntityByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
    }

    public Page<Usuario> getUsuariosByStatus(Status status, Pageable pageable) {
        return usuarioRepository.findByStatus(status, pageable);
    }

    private Usuario createUser(Usuario usuario){
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarUsuarioByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    public boolean existeUsuarioByCorreo(String correo) {
        return usuarioRepository.existsByCorreo(correo);
    }

    public Usuario getUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id: " + id));
    }

    public void actualizarRolUsuario(Usuario usuario, Rol rol) {
        usuario.setRol(rol);
        createUser(usuario);
    }

    // Eliminar usuario
    public void eliminarUsuario(Usuario usuario) {
        usuarioRepository.delete(usuario);
    }

    @Transactional(readOnly = true)
    public Page<Usuario> getTodosUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }
}
