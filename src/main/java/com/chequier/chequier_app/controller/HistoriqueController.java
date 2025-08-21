package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/historique")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class HistoriqueController {

    private final HistoriqueRepository historiqueRepo;
    private final UserRepository userRepo;

    public HistoriqueController(HistoriqueRepository historiqueRepo, UserRepository userRepo) {
        this.historiqueRepo = historiqueRepo;
        this.userRepo = userRepo;
    }

    // 👉 Récupérer l’historique du user connecté
    @GetMapping("/mine")
    public ResponseEntity<List<Historique>> mine(Authentication auth) {
        User u = userRepo.findByEmail(auth.getName()).orElseThrow();
        List<Historique> logs = historiqueRepo.findTop100ByActeur_IdOrderByCreeLeDesc(u.getId());
        return ResponseEntity.ok(logs);
    }

    // 👉 Récupérer l’historique d’une demande spécifique
    @GetMapping("/demande/{id}")
    public ResponseEntity<List<Historique>> byDemande(@PathVariable Long id) {
        List<Historique> logs = historiqueRepo.findByDemande_IdOrderByCreeLeDesc(id);
        return ResponseEntity.ok(logs);
    }

    // 👉 Récupérer l’historique d’un compte spécifique
    @GetMapping("/compte/{id}")
    public ResponseEntity<List<Historique>> byCompte(@PathVariable Long id) {
        List<Historique> logs = historiqueRepo.findByCompte_IdOrderByCreeLeDesc(id);
        return ResponseEntity.ok(logs);
    }
}
