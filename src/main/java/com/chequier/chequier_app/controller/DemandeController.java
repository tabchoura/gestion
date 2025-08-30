package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.DemandeStatut;
import com.chequier.chequier_app.service.DemandeService;
import com.chequier.chequier_app.service.DemandeService.CreateRequest;
import com.chequier.chequier_app.service.DemandeService.UpdateRequest;
import com.chequier.chequier_app.service.DemandeService.DemandeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demandes")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class DemandeController {

    private final DemandeService demandeService;
    public DemandeController(DemandeService demandeService) { this.demandeService = demandeService; }

    // client
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication auth, @PathVariable Long id) {
        try {
            return demandeService.getById(id, auth!=null?auth.getName():null)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error","NotFound","message","Demande introuvable")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // admin
    @GetMapping("/all")
    public ResponseEntity<List<DemandeResponse>> getAll(Authentication auth) {
        try {
            return ResponseEntity.ok(demandeService.getAll(auth!=null?auth.getName():null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // client
    @GetMapping("/mine")
    public ResponseEntity<List<DemandeResponse>> getMine(Authentication auth,
                                                         @RequestParam(required = false) Long compteId) {
        try {
            return ResponseEntity.ok(demandeService.getMine(auth!=null?auth.getName():null, compteId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // client
    @PostMapping
    public ResponseEntity<?> create(Authentication auth, @Valid @RequestBody CreateRequest body) {
        try {
            var out = demandeService.create(auth!=null?auth.getName():null, body);
            return ResponseEntity.status(201).body(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // client
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody UpdateRequest body) {
        try {
            var out = demandeService.update(auth!=null?auth.getName():null, id, body);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // client
    @PutMapping("/{id}/annuler")
    public ResponseEntity<?> annuler(Authentication auth, @PathVariable Long id) {
        try {
            var out = demandeService.annuler(auth!=null?auth.getName():null, id);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        try {
            demandeService.delete(auth!=null?auth.getName():null, id);
            return ResponseEntity.noContent().build(); // 204
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // agent
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> changeStatut(Authentication auth, @PathVariable Long id,
                                          @RequestParam DemandeStatut statut) {
        try {
            var out = demandeService.changeStatut(auth!=null?auth.getName():null, id, statut);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }
}
