package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.Rol;

public record AuthResponse(
        String token,
        String refreshToken,
        Rol role
) {}
