package com.Tapia.ProyectoResidencia.DTO;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String rol,
        String vocalia
) {}

