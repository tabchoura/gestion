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

import io.jsonwebtoken.Claims;
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
    String token = Jwts.builder()
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expHours, ChronoUnit.HOURS)))
        .claims(extraClaims)
        .signWith(key(), Jwts.SIG.HS256)
        .compact();
    
    System.out.println("🎫 Generated token for: " + subject);
    System.out.println("🕒 Token expires at: " + Date.from(now.plus(expHours, ChronoUnit.HOURS)));
    return token;
  }

  public String generateToken(String subject) {
    return generateToken(subject, Map.of());
  }

  public String extractUsername(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(key())
          .build()
          .parseSignedClaims(token)
          .getPayload();
      
      String username = claims.getSubject();
      System.out.println("📧 Extracted username: " + username);
      return username;
    } catch (Exception e) {
      System.out.println("❌ Error extracting username: " + e.getMessage());
      throw e;
    }
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
      String username = extractUsername(token);
      Claims claims = Jwts.parser()
          .verifyWith(key())
          .build()
          .parseSignedClaims(token)
          .getPayload();
      
      Date expiration = claims.getExpiration();
      Date now = new Date();
      
      boolean usernameMatches = username != null && username.equalsIgnoreCase(userDetails.getUsername());
      boolean notExpired = expiration != null && expiration.after(now);
      
      System.out.println("🔍 Token validation:");
      System.out.println("   - Username matches: " + usernameMatches + " (" + username + " vs " + userDetails.getUsername() + ")");
      System.out.println("   - Not expired: " + notExpired + " (expires: " + expiration + ", now: " + now + ")");
      
      return usernameMatches && notExpired;
    } catch (Exception e) {
      System.out.println("❌ Error validating token: " + e.getMessage());
      return false;
    }
  }
}