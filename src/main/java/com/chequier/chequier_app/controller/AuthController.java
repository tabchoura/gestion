package com.chequier.chequier_app.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.UserRepository;
import com.chequier.chequier_app.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final AuthenticationManager authManager;
  private final JwtService jwt;
  private final UserDetailsService uds;

  public AuthController(UserRepository users, PasswordEncoder encoder,
                        AuthenticationManager authManager, JwtService jwt, UserDetailsService uds) {
    this.users = users; this.encoder = encoder; this.authManager = authManager; this.jwt = jwt; this.uds = uds;
  }@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody @Valid RegisterBody body) {
    System.out.println("🟢 REGISTER ENDPOINT CALLED");
    System.out.println("Email: " + body.email);
    
    if (users.existsByEmail(body.email)) {
      return ResponseEntity.badRequest().body(Map.of("error","Email déjà utilisé"));
    }
    
    Role role = body.role == null ? Role.CLIENT : body.role;
    User u = new User(body.nom, body.prenom, body.email, encoder.encode(body.password), role);
    users.save(u);

    // ✅ Correction : passer l'email directement au lieu de UserDetails
    String token = jwt.generateToken(u.getEmail());
    
    System.out.println("✅ USER CREATED SUCCESSFULLY");
    System.out.println("Generated token: " + token.substring(0, Math.min(20, token.length())) + "...");
    
    return ResponseEntity.ok(Map.of(
      "status", 200,
      "message", "Utilisateur créé",
      "token", token,
      "user", Map.of("id", u.getId(), "nom", u.getNom(), "prenom", u.getPrenom(), "email", u.getEmail(), "role", u.getRole().name())
    ));
}

@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody @Valid LoginBody body) {
    System.out.println("🟢 LOGIN ENDPOINT CALLED");
    System.out.println("Email: " + body.email);
    
    try {
      authManager.authenticate(new UsernamePasswordAuthenticationToken(body.email, body.password));
      User u = users.findByEmail(body.email).orElseThrow();
      
      // ✅ Correction : passer l'email directement
      String token = jwt.generateToken(body.email);
      
      System.out.println("✅ LOGIN SUCCESSFUL");
      System.out.println("Generated token: " + token.substring(0, Math.min(20, token.length())) + "...");
      
      return ResponseEntity.ok(Map.of(
        "status", 200,
        "message", "Connexion réussie",
        "token", token,
        "user", Map.of("id", u.getId(), "nom", u.getNom(), "prenom", u.getPrenom(), "email", u.getEmail(), "role", u.getRole().name()),
        "role", u.getRole().name()
      ));
    } catch (Exception e) {
      System.out.println("❌ LOGIN FAILED: " + e.getMessage());
      return ResponseEntity.status(401).body(Map.of("status", 401, "message", "Identifiants invalides"));
    }
}
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest req) {
    if (req.getSession(false) != null) req.getSession(false).invalidate();
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(Map.of("message", "Déconnecté"));
  }

  public static class RegisterBody {
    @NotBlank public String nom;
    @NotBlank public String prenom;
    @Email @NotBlank public String email;
    @NotBlank public String password;
    public Role role;
  }
  public static class LoginBody {
    @Email @NotBlank public String email;
    @NotBlank public String password;
  }
}
