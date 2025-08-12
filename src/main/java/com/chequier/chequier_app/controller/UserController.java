package com.chequier.chequier_app.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  public UserController(UserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  // Helpers simples
  private static final Pattern EMAIL_RX =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  private boolean emailValid(String email) { return email != null && EMAIL_RX.matcher(email).matches(); }
  private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
  private String trimToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
  private ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).body(Map.of("error","Non autorisé")); }
  private ResponseEntity<?> bad(String msg) { return ResponseEntity.badRequest().body(Map.of("message", msg)); }

  private Map<String,Object> userMap(User u) {
    return Map.of(
      "id", u.getId(),
      "nom", u.getNom(),
      "prenom", u.getPrenom(),
      "email", u.getEmail(),
      "role", u.getRole().name()
    );
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(Authentication auth) {
    if (auth == null || auth.getName() == null) return unauthorized();
    User me = users.findByEmail(auth.getName()).orElse(null);
    if (me == null) return unauthorized();
    return ResponseEntity.ok(userMap(me));
  }

  /** Mise à jour partielle du profil (nom/prenom/email) + option de changement de mot de passe */
  @PutMapping("/profile")
  public ResponseEntity<?> updateProfile(@RequestBody Map<String,String> body,
                                         Authentication auth,
                                         HttpServletRequest request) {
    if (auth == null || auth.getName() == null) return unauthorized();
    User me = users.findByEmail(auth.getName()).orElse(null);
    if (me == null) return unauthorized();

    // Récupération des champs (tous optionnels)
    String nom   = trimToNull(body.get("nom"));
    String prenom= trimToNull(body.get("prenom"));
    String email = trimToNull(body.get("email"));

    String currentPassword = trimToNull(body.get("currentPassword"));
    String newPassword     = trimToNull(body.get("newPassword"));

    boolean changed = false;
    boolean passwordChanged = false;

    // nom
    if (nom != null) {
      if (nom.length() < 2) return bad("Nom invalide (min 2 caractères).");
      me.setNom(nom);
      changed = true;
    }

    // prenom
    if (prenom != null) {
      if (prenom.length() < 2) return bad("Prénom invalide (min 2 caractères).");
      me.setPrenom(prenom);
      changed = true;
    }

    // email
    if (email != null) {
      if (!emailValid(email)) return bad("Email invalide.");
      // Unicité uniquement si différent de l’actuel
      if (!email.equalsIgnoreCase(me.getEmail()) && users.existsByEmailAndIdNot(email, me.getId())) {
        return bad("Email déjà utilisé.");
      }
      me.setEmail(email);
      changed = true;
    }

    // Changement de mot de passe (si l’un des 2 champs est fourni, on exige les 2)
    boolean wantsPwdChange = (currentPassword != null) || (newPassword != null);
    if (wantsPwdChange) {
      if (isBlank(currentPassword) || isBlank(newPassword)) {
        return bad("Pour changer le mot de passe, fournissez 'currentPassword' et 'newPassword'.");
      }
      if (!passwordEncoder.matches(currentPassword, me.getPassword())) {
        return bad("Mot de passe actuel incorrect.");
      }
      if (newPassword.length() < 8) {
        return bad("Nouveau mot de passe trop court (min 8 caractères).");
      }
      me.setPassword(passwordEncoder.encode(newPassword));
      changed = true;
      passwordChanged = true;
    }

    if (!changed) {
      return bad("Aucune modification fournie.");
    }

    users.save(me);

    Map<String,Object> resp = new HashMap<>();
    resp.putAll(userMap(me));
    resp.put("passwordChanged", passwordChanged);
    resp.put("message", passwordChanged ? "Profil et mot de passe mis à jour." : "Profil mis à jour.");
    return ResponseEntity.ok(resp);
  }
}
