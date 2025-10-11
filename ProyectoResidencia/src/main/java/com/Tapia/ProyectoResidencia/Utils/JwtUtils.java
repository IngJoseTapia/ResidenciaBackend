package com.Tapia.ProyectoResidencia.Utils;

import com.Tapia.ProyectoResidencia.Model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {

    private final Key signingKey;
    private final long expirationTime;         // en milisegundos
    private final long expirationTimeRefresh;  // en milisegundos
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 60; // 1 min de tolerancia

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationTime,
            @Value("${jwt.expirationRefresh}") long expirationTimeRefresh) {

        byte[] keyBytes = secret.getBytes();
        System.out.println("🔑 Longitud del secret: " + keyBytes.length);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationTime = expirationTime;
        this.expirationTimeRefresh = expirationTimeRefresh;
    }

    // Genera el token principal (access token)
    public String generateToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationTime);

        return Jwts.builder()
                .setSubject(usuario.getCorreo())
                .claim("rol", usuario.getRol().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Genera el refresh token
    public String generateRefreshToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationTimeRefresh);

        return Jwts.builder()
                .setSubject(usuario.getCorreo())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extrae claims y permite margen de tolerancia horaria
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
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
        return username.equals(extractUsername(token)) && isTokenNotExpired(token);
    }

    public boolean isRefreshTokenValid(String token, String username) {
        try {
            return username.equals(extractUsername(token)) && isTokenNotExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenNotExpired(String token) {
        Date expiration = extractClaims(token).getExpiration();
        Instant now = Instant.now();
        return expiration.toInstant().isAfter(now.minusSeconds(ALLOWED_CLOCK_SKEW_SECONDS));
    }
}
