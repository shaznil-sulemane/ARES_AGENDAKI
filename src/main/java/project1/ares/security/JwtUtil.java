package project1.ares.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    public final Key key;
    private final long accessExpiration = 3600000;

    public JwtUtil() {
        String secret = "e471249936e74bb516d6fa4dbbbdff27fa27591be308195b0d4331d28d6ac608";
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Token generateToken(String userId, String role) {
        String accessToken = generateAccessToken(userId, role);
        String refreshToken = generateRefreshToken(userId);
        return new Token(accessToken, refreshToken);
    }

    public String generateAccessToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    public String generateRefreshToken(String userid) {
        long refreshExpiration = accessExpiration * 72;
        return Jwts.builder()
                .setSubject(userid)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    public String generatePendingToken(String userId, String role) {
        long pendingExpiration = 300000;
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + pendingExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Data
    public static class Token {
        private final String accessToken;
        private final String refreshToken;
    }
}
