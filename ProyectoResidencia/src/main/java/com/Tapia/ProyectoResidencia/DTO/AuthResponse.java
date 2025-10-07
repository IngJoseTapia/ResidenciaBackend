package com.Tapia.ProyectoResidencia.DTO;

public record AuthResponse(
        String token,
        String refreshToken
) {}
