package com.marcoromanofinaa.jazzlogs.auth.security;

import com.marcoromanofinaa.jazzlogs.auth.exception.InvalidJwtException;
import com.marcoromanofinaa.jazzlogs.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("displayName", user.getDisplayName())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiration())))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        try {
            return UUID.fromString(parseClaims(token).getSubject());
        } catch (RuntimeException exception) {
            throw new InvalidJwtException();
        }
    }

    public String extractEmail(String token) {
        try {
            return parseClaims(token).get("email", String.class);
        } catch (RuntimeException exception) {
            throw new InvalidJwtException();
        }
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (RuntimeException exception) {
            throw new InvalidJwtException();
        }
    }
}
