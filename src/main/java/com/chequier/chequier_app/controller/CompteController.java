package com.chequier.chequier_app.controller;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.model.CompteBancaire;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.CompteBancaireRepository;
import com.chequier.chequier_app.repository.UserRepository;

@RestController
@RequestMapping("/comptes")
@CrossOrigin
public class CompteController {

    private final CompteBancaireRepository comptes;
    private final UserRepository users;

    public CompteController(CompteBancaireRepository comptes, UserRepository users) {
        this.comptes = comptes;
        this.users = users;
    }

    // Récupère l'utilisateur connecté
    private User current(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + auth.getName()));
    }

    // Sanitize compte pour ne renvoyer que les champs utiles
    private Object sanitize(CompteBancaire c) {
        return new Object() {
            public final Long id = c.getId();
            public final String typeCompte = c.getTypeCompte();
            public final String numeroCompte = c.getNumeroCompte();
            public final String rib = c.getRib();
            public final String iban = c.getIban();
            public final String devise = c.getDevise();
            public final boolean isDefault = c.isDefault();
        };
    }

    // Liste des comptes de l'utilisateur
    @GetMapping({"", "/mine"})
    public ResponseEntity<?> list(Authentication auth) {
        try {
            User u = current(auth);
            var result = comptes.findByUserId(u.getId())
                    .stream()
                    .map(this::sanitize)
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la récupération des comptes: " + e.getMessage());
        }
    }

    // Création d'un compte bancaire
    @PostMapping
    public ResponseEntity<?> create(Authentication auth, @RequestBody CompteBancaire req) {
        try {
            User u = current(auth);
            System.out.println("Création compte pour utilisateur: " + u.getEmail());

            // Validation de base
            if (req.getTypeCompte() == null || req.getTypeCompte().trim().isEmpty())
                return ResponseEntity.badRequest().body("Le type de compte est requis");

            if (req.getNumeroCompte() == null || req.getNumeroCompte().trim().isEmpty())
                return ResponseEntity.badRequest().body("Le numéro de compte est requis");

            req.setTypeCompte(req.getTypeCompte().trim());
            req.setNumeroCompte(req.getNumeroCompte().trim());
            if (req.getRib() != null) req.setRib(req.getRib().trim());
            if (req.getIban() != null) req.setIban(req.getIban().trim());
            if (req.getDevise() != null) req.setDevise(req.getDevise().trim());

            req.setId(null);
            req.setUser(u);

            // Gérer le compte par défaut
            if (req.isDefault()) {
                comptes.findByUserId(u.getId()).stream()
                        .filter(CompteBancaire::isDefault)
                        .forEach(c -> {
                            c.setDefault(false);
                            comptes.save(c);
                        });
            }

            var saved = comptes.save(req);
            return ResponseEntity.ok(sanitize(saved));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la création du compte: " + e.getMessage());
        }
    }

    // Mise à jour d'un compte
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication auth, @PathVariable Long id, @RequestBody CompteBancaire body) {
        try {
            User u = current(auth);
            var c = comptes.findById(id).orElse(null);

            if (c == null || !Objects.equals(c.getUser().getId(), u.getId()))
                return ResponseEntity.notFound().build();

            if (body.getTypeCompte() != null) c.setTypeCompte(body.getTypeCompte().trim());
            if (body.getNumeroCompte() != null) c.setNumeroCompte(body.getNumeroCompte().trim());
            if (body.getRib() != null) c.setRib(body.getRib().trim());
            if (body.getIban() != null) c.setIban(body.getIban().trim());
            if (body.getDevise() != null) c.setDevise(body.getDevise().trim());

            // Gérer le compte par défaut
            if (body.isDefault()) {
                comptes.findByUserId(u.getId()).stream()
                        .filter(CompteBancaire::isDefault)
                        .forEach(x -> {
                            x.setDefault(false);
                            comptes.save(x);
                        });
                c.setDefault(true);
            }

            return ResponseEntity.ok(sanitize(comptes.save(c)));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la mise à jour du compte: " + e.getMessage());
        }
    }

    // Définir un compte par défaut
    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefault(Authentication auth, @PathVariable Long id) {
        try {
            User u = current(auth);
            var c = comptes.findById(id).orElse(null);
            if (c == null || !Objects.equals(c.getUser().getId(), u.getId()))
                return ResponseEntity.notFound().build();

            comptes.findByUserId(u.getId()).stream()
                    .filter(CompteBancaire::isDefault)
                    .forEach(x -> {
                        x.setDefault(false);
                        comptes.save(x);
                    });

            c.setDefault(true);
            return ResponseEntity.ok(sanitize(comptes.save(c)));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors du changement de compte par défaut: " + e.getMessage());
        }
    }

    // Suppression d'un compte
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        try {
            User u = current(auth);
            var c = comptes.findById(id).orElse(null);
            if (c == null || !Objects.equals(c.getUser().getId(), u.getId()))
                return ResponseEntity.notFound().build();

            comptes.delete(c);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la suppression du compte: " + e.getMessage());
        }
    }
}
