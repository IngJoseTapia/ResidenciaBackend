package com.Tapia.ProyectoResidencia.Security;

import com.Tapia.ProyectoResidencia.Model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key signingKey;
    private final long expirationTime;
    private final long expirationTimeRefresh;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationTime,
            @Value("${jwt.expirationRefresh}") long expirationTimeRefresh) {
        byte[] keyBytes = secret.getBytes();
        System.out.println("🔑 Longitud del secret: " + keyBytes.length); // Debug
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationTime = expirationTime;
        this.expirationTimeRefresh = expirationTimeRefresh;
    }

    public String generateToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getCorreo()) // usar el atributo real de tu entidad
                .claim("rol", usuario.getRol().name()) // mismo nombre en generate y extract
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getCorreo())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTimeRefresh)) // 7 días
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("rol", String.class);
    }

    public boolean isTokenValid(String token, String username) {
        return (username.equals(extractUsername(token)) && !isTokenExpired(token));
    }

    public boolean isRefreshTokenValid(String token, String username) {
        try {
            return username.equals(extractUsername(token)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
