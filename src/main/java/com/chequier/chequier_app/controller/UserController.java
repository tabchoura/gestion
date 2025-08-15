package com.chequier.chequier_app.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.repository.UserRepository;
import com.chequier.chequier_app.repository.HistoriqueRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final HistoriqueRepository historiqueRepo;

  public UserController(UserRepository users, PasswordEncoder passwordEncoder, HistoriqueRepository historiqueRepo) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.historiqueRepo = historiqueRepo;
  }

  /* ------------------------- Helpers ------------------------- */

  private void log(User u, String action, String message, String page) {
    if (u == null) return;
    Historique h = new Historique();
    h.setAction(action);               // String
    h.setMessage(message);
    h.setPage(page);
    h.setActeurId(u.getId());
    h.setActeurEmail(u.getEmail());
    historiqueRepo.save(h);
  }

  private static final Pattern EMAIL_RX =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  private boolean emailValid(String email) { return email != null && EMAIL_RX.matcher(email).matches(); }
  private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
  private String trimToNull(String s) { if (s == null) return null; String t = s.trim(); return t.isEmpty() ? null : t; }

  private ResponseEntity<?> unauthorized() {
    return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "JWT manquant ou invalide"));
  }

  private ResponseEntity<?> bad(String msg) {
    return ResponseEntity.badRequest().body(Map.of("message", msg));
  }

  private Map<String, Object> userMap(User u) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", u.getId());
    m.put("nom", u.getNom());
    m.put("prenom", u.getPrenom());
    m.put("email", u.getEmail());
    // si role est un enum, .name(), sinon u.getRole()
    m.put("role", (u.getRole() != null ? u.getRole().toString() : null));
    return m;
  }

  /* ------------------------- Endpoints ------------------------- */

  @GetMapping("/me")
  public ResponseEntity<?> me(Authentication auth) {
    if (auth == null || auth.getName() == null) return unauthorized();
    User me = users.findByEmail(auth.getName()).orElse(null);
    if (me == null) return unauthorized();
    return ResponseEntity.ok(userMap(me));
  }

  @PutMapping("/profile")
  public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body,
                                         Authentication auth,
                                         HttpServletRequest request) {
    if (auth == null || auth.getName() == null) return unauthorized();
    User me = users.findByEmail(auth.getName()).orElse(null);
    if (me == null) return unauthorized();

    String nom            = trimToNull(body.get("nom"));
    String prenom         = trimToNull(body.get("prenom"));
    String email          = trimToNull(body.get("email"));
    String currentPassword= trimToNull(body.get("currentPassword"));
    String newPassword    = trimToNull(body.get("newPassword"));

    boolean changed = false;
    boolean passwordChanged = false;

    if (nom != null) {
      if (nom.length() < 2) return bad("Nom invalide (min 2 caractères).");
      me.setNom(nom); changed = true;
    }
    if (prenom != null) {
      if (prenom.length() < 2) return bad("Prénom invalide (min 2 caractères).");
      me.setPrenom(prenom); changed = true;
    }
    if (email != null) {
      if (!emailValid(email)) return bad("Email invalide.");
      if (!email.equalsIgnoreCase(me.getEmail())
          && users.existsByEmailAndIdNot(email, me.getId())) {
        return bad("Email déjà utilisé.");
      }
      me.setEmail(email); changed = true;
    }

    boolean wantsPwdChange = (currentPassword != null) || (newPassword != null);
    if (wantsPwdChange) {
      if (isBlank(currentPassword) || isBlank(newPassword))
        return bad("Pour changer le mot de passe, fournissez 'currentPassword' et 'newPassword'.");
      if (!passwordEncoder.matches(currentPassword, me.getPassword()))
        return bad("Mot de passe actuel incorrect.");
      if (newPassword.length() < 8)
        return bad("Nouveau mot de passe trop court (min 8 caractères).");
      me.setPassword(passwordEncoder.encode(newPassword));
      changed = true; passwordChanged = true;
    }

    if (!changed) return bad("Aucune modification fournie.");

    users.save(me);

    // log profil
    log(me, "PROFILE_MIS_A_JOUR",
        passwordChanged ? "Profil et mot de passe mis à jour" : "Profil mis à jour",
        "profil");

    Map<String, Object> resp = userMap(me);
    resp.put("passwordChanged", passwordChanged);
    resp.put("message", passwordChanged
        ? "Profil et mot de passe mis à jour."
        : "Profil mis à jour.");

    return ResponseEntity.ok(resp);
  }
}
