package com.Tapia.ProyectoResidencia.Service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PasswordResetAttemptService {

    private final Map<String, List<Instant>> attemptsByEmail = new HashMap<>();
    private final Map<String, List<Instant>> attemptsByIp = new HashMap<>();
    private final int MAX_ATTEMPTS = 3;       // máximo 3 solicitudes
    private final int BLOCK_MINUTES = 15;     // periodo de 15 minutos

    public boolean isBlocked(String email, String ip) {
        cleanOldAttempts(email, ip);
        return (attemptsByEmail.getOrDefault(email, List.of()).size() >= MAX_ATTEMPTS ||
                attemptsByIp.getOrDefault(ip, List.of()).size() >= MAX_ATTEMPTS);
    }

    public void registerAttempt(String email, String ip) {
        attemptsByEmail.computeIfAbsent(email, k -> new ArrayList<>()).add(Instant.now());
        attemptsByIp.computeIfAbsent(ip, k -> new ArrayList<>()).add(Instant.now());
    }

    private void cleanOldAttempts(String email, String ip) {
        Instant cutoff = Instant.now().minus(BLOCK_MINUTES, ChronoUnit.MINUTES);

        attemptsByEmail.computeIfPresent(email, (k, list) -> {
            list.removeIf(time -> time.isBefore(cutoff));
            return list;
        });

        attemptsByIp.computeIfPresent(ip, (k, list) -> {
            list.removeIf(time -> time.isBefore(cutoff));
            return list;
        });
    }
}
