package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetailService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        // Si el usuario se registró con Google y no tiene contraseña, usamos una vacía
        String password = usuario.getContrasena();
        if (password == null) {
            password = ""; // o passwordEncoder.encode("") si tu PasswordEncoder lo requiere
        }

        return User.builder()
                .username(usuario.getCorreo())
                .password(password)
                .roles(usuario.getRol().name()) // Spring Security agrega automáticamente "ROLE_"
                .build();
    }
}
