package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.repository.UserRepository;
import com.chequier.chequier_app.repository.HistoriqueRepository;
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
  private final HistoriqueRepository historiqueRepo;

  public AuthController(UserRepository users,
                        PasswordEncoder encoder,
                        AuthenticationManager authManager,
                        JwtService jwt,
                        HistoriqueRepository historiqueRepo) {
    this.users = users;
    this.encoder = encoder;
    this.authManager = authManager;
    this.jwt = jwt;
    this.historiqueRepo = historiqueRepo;
  }

  private void log(User u, String action, String message, String page) {
    Historique h = new Historique();
    h.setAction(action);              // ⚠️ String, pas ActionType
    h.setMessage(message);
    h.setPage(page);
    h.setActeurId(u.getId());
    h.setActeurEmail(u.getEmail());
    historiqueRepo.save(h);
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

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody @Valid RegisterBody body) {
    try {
      if (users.existsByEmail(body.email)) {
        return ResponseEntity.badRequest().body(Map.of("error","Email déjà utilisé"));
      }

      Role role = (body.role == null ? Role.CLIENT : body.role);
      User u = new User(body.nom, body.prenom, body.email, encoder.encode(body.password), role);
      users.save(u);

      String token = jwt.generateToken(u.getEmail());
      // (Optionnel) log d’inscription :
      // log(u, "REGISTER", "Inscription réussie", "register");

      return ResponseEntity.ok(Map.of(
        "status", 200,
        "message", "Utilisateur créé",
        "token", token,
        "user", Map.of(
          "id", u.getId(),
          "nom", u.getNom(),
          "prenom", u.getPrenom(),
          "email", u.getEmail(),
          "role", u.getRole().name()
        )
      ));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(Map.of("error", "Erreur serveur"));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody @Valid LoginBody body) {
    try {
      authManager.authenticate(new UsernamePasswordAuthenticationToken(body.email, body.password));
      User u = users.findByEmail(body.email).orElseThrow();
      String token = jwt.generateToken(body.email);

      // ✅ log login
      log(u, "LOGIN", "Connexion réussie", "login");

      return ResponseEntity.ok(Map.of(
        "status", 200,
        "message", "Connexion réussie",
        "token", token,
        "user", Map.of(
          "id", u.getId(),
          "nom", u.getNom(),
          "prenom", u.getPrenom(),
          "email", u.getEmail(),
          "role", u.getRole().name()
        ),
        "role", u.getRole().name()
      ));
    } catch (AuthenticationException e) {
      return ResponseEntity.status(401).body(Map.of("error", "Identifiants incorrects"));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(Map.of("error", "Erreur serveur"));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest req) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getName() != null) {
      users.findByEmail(auth.getName()).ifPresent(u -> log(u, "LOGOUT", "Déconnexion", "logout"));
    }
    if (req.getSession(false) != null) req.getSession(false).invalidate();
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(Map.of("message", "Déconnecté"));
  }
}
