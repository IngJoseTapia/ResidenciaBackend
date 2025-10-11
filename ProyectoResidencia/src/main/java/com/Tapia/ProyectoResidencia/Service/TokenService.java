package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Model.PasswordResetToken;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final PasswordResetTokenRepository tokenRepository;

    public void eliminarTokenUsuario(Usuario usuario){
        tokenRepository.deleteByUsuario(usuario);
    }

    public String obtenerToken(Usuario usuario){
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUsuario(usuario);
        resetToken.setExpiryDate(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)));
        tokenRepository.save(resetToken);
        return token;
    }

    public Optional<PasswordResetToken> buscarToken(String token){
        return tokenRepository.findByToken(token);
    }

    public void eliminarToken(PasswordResetToken token){
        tokenRepository.delete(token);
    }
}
