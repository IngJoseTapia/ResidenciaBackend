package com.Tapia.ProyectoResidencia.Utils;

public class PasswordUtils {

    private PasswordUtils() {
        // Evita instanciación
        throw new IllegalStateException("Utility class");
    }

    /**
     * Valída fuerza de contraseña:
     * Al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.
     */
    public static boolean isWeakPassword(String password) {
        if (password == null) return true;
        return !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    }
}
