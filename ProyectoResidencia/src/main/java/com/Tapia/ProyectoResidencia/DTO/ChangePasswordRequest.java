package com.Tapia.ProyectoResidencia.DTO;

public record ChangePasswordRequest(
        String passwordActual,
        String nuevaPassword,
        String confirmarPassword
) {}
