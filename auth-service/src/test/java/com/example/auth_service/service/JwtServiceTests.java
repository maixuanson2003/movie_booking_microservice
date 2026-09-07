// package com.example.auth_service.service;

// import static org.junit.jupiter.api.Assertions.*;

// import java.time.Duration;
// import java.time.Instant;
// import java.util.Date;

// import org.junit.jupiter.api.Test;

// import com.example.auth_service.sharedLogic.dto.AuthInfo;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.SignatureAlgorithm;
// import io.jsonwebtoken.io.Decoders;
// import io.jsonwebtoken.security.Keys;

// class JwtServiceTests {
// private static final String SECRET =
// "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
// private final JwtService service = new JwtService(SECRET,
// Duration.ofMinutes(30));

// @Test
// void tokenContainsIdentityCurrentIssuedAtAndMatchingRole() {
// Instant before = Instant.now().minusSeconds(1);
// String token = service.createToken(new AuthInfo(1L, "alice",
// "alice@example.com", "Alice", "USER"));
// var claims = service.getAllClaim(token);
// assertEquals("alice", claims.getSubject());
// assertTrue(claims.getIssuedAt().toInstant().isAfter(before));
// assertEquals(1_800_000L, claims.getExpiration().getTime() -
// claims.getIssuedAt().getTime());
// assertTrue(service.isTokenValid(token));
// assertTrue(service.validRoleUser(token, new String[] { "user" }));
// assertFalse(service.validRoleUser(token, new String[] { "ADMIN" }));
// }

// @Test
// void rejectsExpiredMalformedAndWrongSignatureTokens() {
// String expired = Jwts.builder().setSubject("alice").claim("role", "USER")
// .setExpiration(Date.from(Instant.now().minusSeconds(60)))
// .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)),
// SignatureAlgorithm.HS256).compact();
// String wrongKey = Jwts.builder().setSubject("alice").claim("role", "USER")
// .setExpiration(Date.from(Instant.now().plusSeconds(60)))
// .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256)).compact();
// for (String token : new String[] { expired, wrongKey, "invalid", "", null })
// {
// assertFalse(service.isTokenValid(token));
// assertFalse(service.validRoleUser(token, new String[] { "USER" }));
// }
// }

// @Test
// void rejectsMissingOrWeakKeysAndNonpositiveLifetime() {
// assertThrows(IllegalArgumentException.class, () -> new JwtService("",
// Duration.ofMinutes(30)));
// assertThrows(io.jsonwebtoken.security.WeakKeyException.class,
// () -> new JwtService("YWJj", Duration.ofMinutes(30)));
// assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET,
// Duration.ZERO));
// }
// }
