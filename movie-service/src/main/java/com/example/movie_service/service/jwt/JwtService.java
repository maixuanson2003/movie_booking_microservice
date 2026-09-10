package com.example.movie_service.service.jwt;

import java.security.Key;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import com.example.movie_service.sharedLogic.dto.AuthInfo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {
    public static final String SECRET = "NzgzNzI2NDc1MzE1NTg4NzEyNjE1MjM3ODQ5MjA2MDM0NzY1MTExMjM0NTY3";

    public String createToken(AuthInfo authInfo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("auth", authInfo);

        return GenerateToken(claims, authInfo.getUsername());
    }

    private String GenerateToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(0))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();

    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token) {
        Claims claim = Jwts.parserBuilder().setSigningKey(getSignKey()).build()
                .parseClaimsJws(token).getBody();
        return !claim.getExpiration().before(new Date(0));
    }

    public boolean validRoleUser(String token, String[] role) {
        Claims claim = Jwts.parserBuilder().setSigningKey(getSignKey()).build()
                .parseClaimsJws(token).getBody();
        String roleAuth = claim.get("role", String.class);
        for (String rol : role) {
            if (rol.equalsIgnoreCase(roleAuth)) {
                return true;
            }
        }
        return false;
    }

    public Claims getAllClaim(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token)
                .getBody();
    }

    public static Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(Decoders.BASE64.decode(SECRET)).build()
                .parseClaimsJws(token).getBody();
    }

}

