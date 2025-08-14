package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;
import com.chequier.chequier_app.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final AuthenticationManager authManager;
  private final JwtService jwt;
  private final HistoriqueRepository histo;

  public AuthController(UserRepository users,
                        PasswordEncoder encoder,
                        AuthenticationManager authManager,
                        JwtService jwt,
                        HistoriqueRepository histo) {
    this.users = users;
    this.encoder = encoder;
    this.authManager = authManager;
    this.jwt = jwt;
    this.histo = histo;
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

  // Log utilitaire
  private void logAction(String email, String role, Historique.RessourceType type,
                         Historique.ActionType action, String message, String payload) {
    var h = new Historique();
    h.setActeurEmail(email);
    h.setActeurRole(role);
    h.setRessourceType(type);
    h.setAction(action);
    h.setMessage(message);
    h.setPayloadJson(payload);
    histo.save(h);
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody @Valid RegisterBody body) {
    try {
      if (users.existsByEmail(body.email)) { // ✅ compile maintenant
        logAction(body.email, "ANONYME", Historique.RessourceType.AUTH,
          Historique.ActionType.CREER, "Tentative inscription - Email déjà utilisé",
          String.format("{\"email\":\"%s\"}", body.email));
        return ResponseEntity.badRequest().body(Map.of("error","Email déjà utilisé"));
      }

      Role role = body.role == null ? Role.CLIENT : body.role;
      User u = new User(body.nom, body.prenom, body.email, encoder.encode(body.password), role);
      users.save(u);

      String token = jwt.generateToken(u.getEmail());

      logAction(u.getEmail(), role.name(), Historique.RessourceType.AUTH,
        Historique.ActionType.CREER, "Inscription réussie",
        String.format("{\"nom\":\"%s\",\"prenom\":\"%s\",\"role\":\"%s\"}",
          body.nom, body.prenom, role.name()));

      return ResponseEntity.ok(Map.of(
        "status", 200,
        "message", "Utilisateur créé",
        "token", token,
        "user", Map.of("id", u.getId(), "nom", u.getNom(), "prenom", u.getPrenom(),
                       "email", u.getEmail(), "role", u.getRole().name())
      ));
    } catch (Exception e) {
      logAction(body.email, "ANONYME", Historique.RessourceType.AUTH,
        Historique.ActionType.CREER, "Erreur lors de l'inscription: " + e.getMessage(),
        String.format("{\"error\":\"%s\"}", e.getMessage()));
      return ResponseEntity.internalServerError().body(Map.of("error", "Erreur serveur"));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody @Valid LoginBody body) {
    try {
      authManager.authenticate(new UsernamePasswordAuthenticationToken(body.email, body.password));
      User u = users.findByEmail(body.email).orElseThrow();
      String token = jwt.generateToken(body.email);

      logAction(u.getEmail(), u.getRole().name(), Historique.RessourceType.AUTH,
        Historique.ActionType.CONNEXION, "Connexion réussie",
        String.format("{\"userAgent\":\"%s\"}", "Web App"));

      return ResponseEntity.ok(Map.of(
        "status", 200,
        "message", "Connexion réussie",
        "token", token,
        "user", Map.of("id", u.getId(), "nom", u.getNom(), "prenom", u.getPrenom(),
                       "email", u.getEmail(), "role", u.getRole().name()),
        "role", u.getRole().name()
      ));
    } catch (AuthenticationException e) { // 👈 multicatch conseillé par l'IDE
      logAction(body.email, "ANONYME", Historique.RessourceType.AUTH,
        Historique.ActionType.CONNEXION, "Tentative de connexion échouée",
        "{\"error\":\"Identifiants incorrects\"}");
      return ResponseEntity.status(401).body(Map.of("error", "Identifiants incorrects"));
    } catch (Exception e) {
      logAction(body.email, "ANONYME", Historique.RessourceType.AUTH,
        Historique.ActionType.CONNEXION, "Erreur lors de la connexion: " + e.getMessage(),
        String.format("{\"error\":\"%s\"}", e.getMessage()));
      return ResponseEntity.internalServerError().body(Map.of("error", "Erreur serveur"));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest req, @org.springframework.lang.Nullable org.springframework.security.core.Authentication auth) {
    String email = (auth != null && auth.getName() != null) ? auth.getName() : "inconnu";
    String role = "UNKNOWN";
    if (auth != null && auth.getName() != null) {
      var u = users.findByEmail(auth.getName()).orElse(null);
      if (u != null) role = u.getRole().name();
      logAction(email, role, Historique.RessourceType.AUTH,
        Historique.ActionType.DECONNEXION, "Déconnexion", null);
    }
    if (req.getSession(false) != null) req.getSession(false).invalidate();
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(Map.of("message", "Déconnecté"));
  }
}
