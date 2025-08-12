package com.chequier.chequier_app.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

  @Value("${security.jwt.secret}")
  private String secret;

  @Value("${security.jwt.exp-hours:24}")
  private int expHours;

  private SecretKey key() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(String subject, Map<String, Object> extraClaims) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expHours, ChronoUnit.HOURS)))
        .claims(extraClaims)
        .signWith(key(), Jwts.SIG.HS256)
        .compact();
  }

  public String generateToken(String subject) {
    return generateToken(subject, Map.of());
  }

  public String extractUsername(String token) {
    return Jwts.parser().verifyWith(key()).build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    String username = extractUsername(token);
    Date exp = Jwts.parser().verifyWith(key()).build()
        .parseSignedClaims(token)
        .getPayload()
        .getExpiration();
    return username != null
        && username.equalsIgnoreCase(userDetails.getUsername())
        && exp != null
        && exp.after(new Date());
  }
}
