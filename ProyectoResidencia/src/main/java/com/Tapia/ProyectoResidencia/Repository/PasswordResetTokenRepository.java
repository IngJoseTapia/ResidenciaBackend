package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.PasswordResetToken;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUsuario(Usuario usuario); // Para invalidar tokens antiguos
}
