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
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest req,
      @NonNull HttpServletResponse res,
      @NonNull FilterChain chain
  ) throws ServletException, IOException {

    // Debug logs
    System.out.println("🔥 === JWT FILTER START ===");
    System.out.println("🔍 Method: " + req.getMethod());
    System.out.println("🔍 URI: " + req.getRequestURI());
    System.out.println("🔍 Authorization header: " + req.getHeader("Authorization"));
    
    // Skip JWT validation for auth endpoints
    if (req.getRequestURI().startsWith("/auth/")) {
        System.out.println("🟢 AUTH ENDPOINT - SKIPPING JWT VALIDATION");
        chain.doFilter(req, res);
        return;
    }
    
    // Skip JWT validation for OPTIONS requests (CORS preflight)
    if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
        System.out.println("🟢 OPTIONS REQUEST - SKIPPING JWT VALIDATION");
        chain.doFilter(req, res);
        return;
    }
    
    // Skip JWT validation for error endpoints
    if (req.getRequestURI().startsWith("/error")) {
        System.out.println("🟢 ERROR ENDPOINT - SKIPPING JWT VALIDATION");
        chain.doFilter(req, res);
        return;
    }
    
    final String authHeader = req.getHeader("Authorization");
    if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
        System.out.println("❌ NO AUTH HEADER OR WRONG FORMAT");
        System.out.println("🔚 === JWT FILTER END (NO AUTH) ===");
        // Let Spring Security handle the authentication failure
        chain.doFilter(req, res);
        return;
    }

    final String jwt = authHeader.substring(7).trim();
    System.out.println("🎫 JWT Token: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");

    try {
        String email = jwtService.extractUsername(jwt);
        System.out.println("📧 Extracted email: " + email);
        
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("👤 Loading user details for: " + email);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            
            System.out.println("🔐 Validating token...");
            if (jwtService.isTokenValid(jwt, userDetails)) {
                var authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("✅ AUTHENTICATION SET SUCCESSFULLY for: " + email);
                System.out.println("👮‍♂️ Authorities: " + userDetails.getAuthorities());
            } else {
                System.out.println("❌ TOKEN NOT VALID");
            }
        } else if (email == null) {
            System.out.println("❌ EMAIL NULL FROM TOKEN");
        } else {
            System.out.println("ℹ️ USER ALREADY AUTHENTICATED");
        }
    } catch (JwtException ex) {
        System.out.println("❌ JWT EXCEPTION: " + ex.getMessage());
    } catch (IllegalArgumentException ex) {
        System.out.println("❌ ILLEGAL ARGUMENT EXCEPTION: " + ex.getMessage());
    } catch (Exception ex) {
        System.out.println("❌ UNEXPECTED EXCEPTION: " + ex.getMessage());
        ex.printStackTrace();
    }

    System.out.println("🔚 === JWT FILTER END ===");
    chain.doFilter(req, res);
  }
}