package com.chequier.chequier_app.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;

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


    private void log(User u, String action, String message, String page) {
        if (u == null) return;
        Historique h = new Historique();
        h.setAction(action);
        h.setMessage(message);
        h.setPage(page);
h.setActeur(u);                    // ✅ relation ManyToOne vers User

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
        m.put("role", (u.getRole() != null ? u.getRole().toString() : null));
        m.put("numCin", u.getNumCin());
        m.put("numCompteBancaire", u.getNumCompteBancaire());
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

    // @PostMapping("/register")
    // public ResponseEntity<?> register(@RequestBody Map<String, String> body) {

    //     String nom = trimToNull(body.get("nom"));
    //     String prenom = trimToNull(body.get("prenom"));
    //     String email = trimToNull(body.get("email"));
    //     String password = trimToNull(body.get("password"));
    //     String numCin = trimToNull(body.get("numCin"));
    //     String numCompteBancaire = trimToNull(body.get("numCompteBancaire"));

    //     if (nom == null || prenom == null || email == null || password == null || numCin == null || numCompteBancaire == null)
    //         return bad("Tous les champs sont obligatoires.");

    //     if (!EMAIL_RX.matcher(email).matches()) return bad("Email invalide.");
    //     if (!numCin.matches("^[0-9]{8}$")) return bad("CIN invalide (8 chiffres).");
    //     if (!numCompteBancaire.matches("^[0-9]{10,20}$")) return bad("Compte bancaire invalide (10 à 20 chiffres).");

    //     if (users.existsByEmail(email)) return bad("Email déjà utilisé.");
    //     if (users.existsByNumCin(numCin)) return bad("CIN déjà utilisé.");
    //     if (users.existsByNumCompteBancaire(numCompteBancaire)) return bad("Compte bancaire déjà utilisé.");

    //     User u = new User(nom, prenom, email, passwordEncoder.encode(password), Role.CLIENT, numCin, numCompteBancaire);
    //     users.save(u);

    //     log(u, "REGISTER", "Nouvel utilisateur créé", "register");

    //     return ResponseEntity.ok(Map.of("message", "Utilisateur créé avec succès", "user", userMap(u)));
    // }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body,
                                           Authentication auth,
                                           HttpServletRequest request) {
        if (auth == null || auth.getName() == null) return unauthorized();
        User me = users.findByEmail(auth.getName()).orElse(null);
        if (me == null) return unauthorized();

        String nom             = trimToNull(body.get("nom"));
        String prenom          = trimToNull(body.get("prenom"));
        String email           = trimToNull(body.get("email"));
        String currentPassword = trimToNull(body.get("currentPassword"));
        String newPassword     = trimToNull(body.get("newPassword"));
        String numCin          = trimToNull(body.get("numCin"));
        String numCompteBancaire = trimToNull(body.get("numCompteBancaire"));

        boolean changed = false;

        if (nom != null && !nom.equals(me.getNom())) { me.setNom(nom); changed = true; }
        if (prenom != null && !prenom.equals(me.getPrenom())) { me.setPrenom(prenom); changed = true; }

        if (email != null && !email.equals(me.getEmail())) {
            if (!EMAIL_RX.matcher(email).matches()) return bad("Email invalide.");
            if (users.existsByEmailAndIdNot(email, me.getId())) return bad("Email déjà utilisé.");
            me.setEmail(email); changed = true;
        }

        if (numCin != null) {
            if (!numCin.matches("^[0-9]{8}$")) return bad("CIN invalide (8 chiffres).");
            if (!numCin.equals(me.getNumCin()) && users.existsByNumCinAndIdNot(numCin, me.getId()))
                return bad("CIN déjà utilisé.");
            me.setNumCin(numCin); changed = true;
        }

        if (numCompteBancaire != null) {
            if (!numCompteBancaire.matches("^[0-9]{10,20}$")) return bad("Compte bancaire invalide (10 à 20 chiffres).");
            if (!numCompteBancaire.equals(me.getNumCompteBancaire()) && users.existsByNumCompteBancaireAndIdNot(numCompteBancaire, me.getId()))
                return bad("Compte bancaire déjà utilisé.");
            me.setNumCompteBancaire(numCompteBancaire); changed = true;
        }

        if (newPassword != null) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, me.getPassword()))
                return bad("Mot de passe actuel invalide.");
            me.setPassword(passwordEncoder.encode(newPassword)); changed = true;
        }

        if (changed) {
            users.save(me);
            log(me, "UPDATE_PROFILE", "Profil mis à jour", "profile");
        }

        return ResponseEntity.ok(Map.of("message", "Profil mis à jour avec succès", "user", userMap(me)));
    }
}
