package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.ChangePasswordRequest;
import com.Tapia.ProyectoResidencia.DTO.UpdateUserRequest;
import com.Tapia.ProyectoResidencia.DTO.UserResponse;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UsuarioRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UsuarioRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getUserByEmail(String email) {
        var user = userRepository.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return new UserResponse(
                user.getNombre(),
                user.getApellidoPaterno(),
                user.getApellidoMaterno(),
                user.getCorreo(),
                user.getTelefono(),
                user.getGenero()
        );
    }

    public void updateUser(String email, UpdateUserRequest request) {
        var user = userRepository.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setNombre(request.nombre());
        user.setApellidoPaterno(request.apellidoPaterno());
        user.setApellidoMaterno(request.apellidoMaterno());
        user.setGenero(request.genero());
        user.setTelefono(request.telefono());
        userRepository.save(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        var user = userRepository.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.passwordActual(), user.getContrasena())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        user.setContrasena(passwordEncoder.encode(request.nuevaPassword()));
        userRepository.save(user);
    }
}
