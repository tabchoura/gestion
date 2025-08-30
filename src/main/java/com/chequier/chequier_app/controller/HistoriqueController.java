package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.service.HistoriqueService;
import com.chequier.chequier_app.service.HistoriqueService.HistoriqueResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/historique")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class HistoriqueController {

    private final HistoriqueService historiqueService;

    public HistoriqueController(HistoriqueService historiqueService) {
        this.historiqueService = historiqueService;
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(Authentication auth) {
        try {
            var out = historiqueService.getUserHistory(auth != null ? auth.getName() : null);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    @GetMapping("/demande/{id}")
    public ResponseEntity<?> byDemande(Authentication auth, @PathVariable Long id) {
        try {
            var out = historiqueService.getHistoryByDemande(auth != null ? auth.getName() : null, id);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    @GetMapping("/compte/{id}")
    public ResponseEntity<?> byCompte(Authentication auth, @PathVariable Long id) {
        try {
            var out = historiqueService.getHistoryByCompte(auth != null ? auth.getName() : null, id);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }
}
