package com.chequier.chequier_app.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;


  
  public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }@Override
protected void doFilterInternal(
    @NonNull HttpServletRequest req,
    @NonNull HttpServletResponse res,
    @NonNull FilterChain chain
) throws ServletException, IOException {

    // AJOUT DES LOGS DE DEBUG
    System.out.println("=== JWT FILTER DEBUG ===");
    System.out.println("Method: " + req.getMethod());
    System.out.println("URI: " + req.getRequestURI());
    System.out.println("Authorization header: " + req.getHeader("Authorization"));
    
    // ✅ Ignorer les endpoints d'authentification
    if (req.getRequestURI().startsWith("/auth/")) {
        System.out.println("🟢 AUTH ENDPOINT - SKIPPING JWT VALIDATION");
        chain.doFilter(req, res);
        return;
    }
    
    System.out.println("========================");

    final String h = req.getHeader("Authorization");
    if (h == null || !h.regionMatches(true, 0, "Bearer ", 0, 7)) {
        System.out.println("❌ NO AUTH HEADER OR WRONG FORMAT");
        chain.doFilter(req, res);
        return;
    }

    final String jwt = h.substring(7).trim();
    System.out.println("JWT Token: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");

    try {
        String email = jwtService.extractUsername(jwt);
        System.out.println("Extracted email: " + email);
        
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails details = userDetailsService.loadUserByUsername(email);
            if (jwtService.isTokenValid(jwt, details)) {
                var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
                System.out.println("✅ AUTHENTICATION SET SUCCESSFULLY");
            } else {
                System.out.println("❌ TOKEN NOT VALID");
            }
        } else {
            System.out.println("❌ EMAIL NULL OR ALREADY AUTHENTICATED");
        }
    } catch (JwtException | IllegalArgumentException ex) {
        System.out.println("❌ JWT EXCEPTION: " + ex.getMessage());
    }

    chain.doFilter(req, res);
}
}
