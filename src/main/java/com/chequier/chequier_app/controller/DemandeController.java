package com.chequier.chequier_app.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.model.DemandeChequier;
import com.chequier.chequier_app.model.DemandeStatut;
import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.CompteBancaireRepository;
import com.chequier.chequier_app.repository.DemandeChequierRepository;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/demandes")
// @CrossOrigin(origins = "*")
public class DemandeController {

    private final DemandeChequierRepository demandes;
    private final UserRepository users;
    private final CompteBancaireRepository comptes;
    private final HistoriqueRepository historiqueRepo;

    public DemandeController(DemandeChequierRepository demandes,
                             UserRepository users,
                             CompteBancaireRepository comptes,
                             HistoriqueRepository historiqueRepo) {
        this.demandes = demandes;
        this.users = users;
        this.comptes = comptes;
        this.historiqueRepo = historiqueRepo;
    }

    private User current(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Authentification requise");
        }
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + auth.getName()));
    }

    private void log(User u, String action, String message, String page) {
        try {
            if (u != null && historiqueRepo != null) {
                Historique h = new Historique();
                h.setAction(action);
                h.setMessage(message);
                h.setPage(page);
h.setActeur(u);                    // ✅ relation ManyToOne vers User

                historiqueRepo.save(h);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de l'historique: " + e.getMessage());
        }
    }

    // =========================
    // GET: récupérer une demande par ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication auth, @PathVariable Long id) {
        try {
            System.out.println("=== GET DEMANDE BY ID ===");
            System.out.println("ID demandé: " + id);
            System.out.println("Auth: " + (auth != null ? auth.getName() : "null"));
            
            var me = current(auth);
            System.out.println("Utilisateur connecté: " + me.getEmail() + " - Role: " + me.getRole());
            
            var demande = demandes.findById(id).orElse(null);
            
            if (demande == null) {
                System.out.println("Demande non trouvée avec l'ID: " + id);
                return ResponseEntity.status(404)
                    .body(Map.of("error", "NotFound", "message", "Demande introuvable avec l'ID: " + id));
            }
            
            System.out.println("Demande trouvée: " + demande.getId() + " - User: " + demande.getUser().getEmail());
            
            // Vérification des permissions
            if (me.getRole() == Role.AGENT) {
                System.out.println("Accès accordé (AGENT)");
                return ResponseEntity.ok(demande);
            } else {
                if (!demande.getUser().getId().equals(me.getId())) {
                    System.out.println("Accès refusé - la demande n'appartient pas à l'utilisateur");
                    return ResponseEntity.status(403)
                        .body(Map.of("error", "Forbidden", "message", "Vous n'avez pas accès à cette demande"));
                }
                System.out.println("Accès accordé (propriétaire)");
                return ResponseEntity.ok(demande);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur dans getById: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // =========================
    // GET: TOUTES les demandes (VERSION FINALE)
    // =========================
    @GetMapping("/all")
    public ResponseEntity<?> getAll(Authentication auth) {
        try {
            System.out.println("=== GET ALL DEMANDES ===");
            System.out.println("Auth: " + (auth != null ? auth.getName() : "null"));
            
            var me = current(auth);
            System.out.println("Utilisateur connecté: " + me.getEmail() + " - Role: " + me.getRole());
            
            List<DemandeChequier> out;
            
            if (me.getRole() == Role.AGENT) {
                // AGENT : peut voir toutes les demandes
                System.out.println("Récupération de toutes les demandes (AGENT)...");
                log(me, "CONSULTATION_DEMANDES", "Consultation de toutes les demandes (AGENT)", "demandes");
                
                try {
                    // Essayez avec le tri
                    out = demandes.findAllByOrderByCreatedAtDesc();
                    System.out.println("Méthode avec tri réussie - Nombre de demandes: " + out.size());
                } catch (Exception e) {
                    System.err.println("Erreur avec tri, utilisation de findAll(): " + e.getMessage());
                    out = demandes.findAll();
                    System.out.println("Fallback réussi - Nombre de demandes: " + out.size());
                }
            } else {
                // CLIENT : ne voit que ses propres demandes
                System.out.println("Récupération des demandes du client uniquement...");
                log(me, "CONSULTATION_DEMANDES", "Consultation de mes demandes (CLIENT)", "demandes");
                
                try {
                    // Essayez avec le tri
                    out = demandes.findByUser_IdOrderByCreatedAtDesc(me.getId());
                    System.out.println("Méthode avec tri réussie - Nombre de demandes: " + out.size());
                } catch (Exception e) {
                    System.err.println("Erreur avec tri, utilisation de findByUser_Id(): " + e.getMessage());
                    try {
                        out = demandes.findByUser_Id(me.getId());
                        System.out.println("Fallback réussi - Nombre de demandes: " + out.size());
                    } catch (Exception e2) {
                        System.err.println("Erreur même avec fallback: " + e2.getMessage());
                        out = Collections.emptyList();
                    }
                }
            }
            
            return ResponseEntity.ok(out != null ? out : Collections.emptyList());
            
        } catch (Exception e) {
            System.err.println("=== ERREUR GLOBALE GET ALL DEMANDES ===");
            System.err.println("Message: " + e.getMessage());
            System.err.println("Classe: " + e.getClass().getSimpleName());
            e.printStackTrace();
            System.err.println("=== FIN ERREUR ===");
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // =========================
    // GET: mes demandes (VERSION FINALE)
    // =========================
    @GetMapping({"/mine", "", "/my"})
    public ResponseEntity<?> getMine(Authentication auth,
                                   @RequestParam(required = false) Long compteId) {
        try {
            System.out.println("=== GET MES DEMANDES ===");
            System.out.println("Auth: " + (auth != null ? auth.getName() : "null"));
            System.out.println("CompteId filter: " + compteId);
            
            var me = current(auth);
            System.out.println("Utilisateur connecté: " + me.getEmail());

            List<DemandeChequier> result;
            
            if (compteId != null) {
                var cptOpt = comptes.findById(compteId);
                if (cptOpt.isEmpty() || cptOpt.get().getUser() == null || !cptOpt.get().getUser().getId().equals(me.getId())) {
                    System.out.println("Compte non trouvé ou n'appartient pas à l'utilisateur");
                    return ResponseEntity.status(404).body(Collections.emptyList());
                }
                
                try {
                    result = demandes.findByCompte_IdOrderByCreatedAtDesc(compteId);
                } catch (Exception e) {
                    System.err.println("Erreur avec tri par compte, fallback: " + e.getMessage());
                    result = demandes.findByCompte_Id(compteId);
                }
            } else {
                try {
                    result = demandes.findByUser_IdOrderByCreatedAtDesc(me.getId());
                } catch (Exception e) {
                    System.err.println("Erreur avec tri par user, fallback: " + e.getMessage());
                    result = demandes.findByUser_Id(me.getId());
                }
            }
            
            System.out.println("Nombre de demandes trouvées: " + (result != null ? result.size() : 0));
            return ResponseEntity.ok(result != null ? result : Collections.emptyList());
            
        } catch (Exception e) {
            System.err.println("Erreur dans getMine: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // =========================
    // CREATE
    // =========================
    public static class CreateBody {
        public @NotNull Long compteId;
        public LocalDate dateDemande;
        public @NotNull Integer pages;
        public String motif;
        
        @Override
        public String toString() {
            return "CreateBody{compteId=" + compteId + ", dateDemande=" + dateDemande + 
                   ", pages=" + pages + ", motif='" + motif + "'}";
        }
    }

    @PostMapping
    public ResponseEntity<?> create(Authentication auth, @Valid @RequestBody CreateBody body) {
        try {
            System.out.println("=== CREATE DEMANDE ===");
            System.out.println("Body: " + body);
            
            var me = current(auth);

            if (body.compteId == null)
                return ResponseEntity.badRequest().body(Map.of("error","Validation","message","compteId est requis"));
            if (body.pages == null || body.pages <= 0)
                return ResponseEntity.badRequest().body(Map.of("error","Validation","message","pages doit être > 0"));

            var cpt = comptes.findById(body.compteId).orElse(null);
            if (cpt == null)
                return ResponseEntity.status(404).body(Map.of("error","NotFound","message","Compte introuvable"));
            if (cpt.getUser() == null || !cpt.getUser().getId().equals(me.getId()))
                return ResponseEntity.status(403).body(Map.of("error","Forbidden","message","Ce compte n'appartient pas à l'utilisateur"));

            var d = new DemandeChequier();
            d.setUser(me);
            d.setCompte(cpt);
            d.setDateDemande(body.dateDemande != null ? body.dateDemande : LocalDate.now());
            d.setPages(body.pages);
            d.setMotif(body.motif);
            d.setStatut(DemandeStatut.EN_ATTENTE);

            d = demandes.save(d);
            log(me, "DEMANDE_CREE", "Demande #" + d.getId(), "demandes");
            
            System.out.println("Demande créée avec succès: ID " + d.getId());
            return ResponseEntity.status(201).body(d);

        } catch (DataIntegrityViolationException e) {
            var msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
            System.err.println("Erreur d'intégrité: " + msg);
            return ResponseEntity.status(409).body(Map.of("error","DataIntegrityViolation","message", msg));
        } catch (Exception e) {
            System.err.println("Erreur lors de la création: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // =========================
    // UPDATE
    // =========================
    public static class UpdateBody {
        public LocalDate dateDemande;
        public Integer pages;
        public String motif;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody UpdateBody body) {
        try {
            var me = current(auth);
            var d = demandes.findById(id).orElse(null);
            if (d == null) return ResponseEntity.notFound().build();
            if (!d.getUser().getId().equals(me.getId())) return ResponseEntity.status(403).build();
            if (d.getStatut() != DemandeStatut.EN_ATTENTE) 
                return ResponseEntity.badRequest().body(Map.of("error","InvalidState","message","Seules les demandes EN_ATTENTE sont éditables"));

            if (body.dateDemande != null) d.setDateDemande(body.dateDemande);
            if (body.pages != null) {
                if (body.pages <= 0) return ResponseEntity.badRequest().body(Map.of("error","Validation","message","pages doit être > 0"));
                d.setPages(body.pages);
            }
            if (body.motif != null) d.setMotif(body.motif);

            d = demandes.save(d);
            log(me, "DEMANDE_MISE_A_JOUR", "Demande #" + d.getId(), "demandes");
            return ResponseEntity.ok(d);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // =========================
    // ANNULER
    // =========================
    @PutMapping("/{id}/annuler")
    public ResponseEntity<?> annuler(Authentication auth, @PathVariable Long id) {
        try {
            var me = current(auth);
            var d = demandes.findById(id).orElse(null);
            if (d == null) return ResponseEntity.notFound().build();
            if (!d.getUser().getId().equals(me.getId())) return ResponseEntity.status(403).build();
            if (d.getStatut() != DemandeStatut.EN_ATTENTE)
                return ResponseEntity.badRequest().body(Map.of("error","InvalidState","message","Impossible d'annuler : statut " + d.getStatut()));

            d.setStatut(DemandeStatut.ANNULEE);
            d = demandes.save(d);
            log(me, "DEMANDE_ANNULEE", "Demande #" + d.getId(), "demandes");
            return ResponseEntity.ok(d);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // =========================
    // SUPPRIMER (Agent uniquement)
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        try {
            var me = current(auth);
            if (me.getRole() != Role.AGENT) return ResponseEntity.status(403).build();
            var d = demandes.findById(id).orElse(null);
            if (d == null) return ResponseEntity.notFound().build();
            demandes.delete(d);
            log(me, "DEMANDE_SUPPRIMEE", "Demande #" + id, "demandes");
            return ResponseEntity.ok(Map.of("deleted", id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }

    // =========================
    // AGENT: Approuver / Rejeter
    // =========================
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> changeStatut(Authentication auth, @PathVariable Long id,
                                          @RequestParam DemandeStatut statut) {
        try {
            var me = current(auth);
            if (me.getRole() != Role.AGENT) return ResponseEntity.status(403).build();
            var d = demandes.findById(id).orElse(null);
            if (d == null) return ResponseEntity.notFound().build();

            d.setStatut(statut);
            d = demandes.save(d);
            log(me, "DEMANDE_STATUT_CHANGE", "Demande #" + id + " → " + statut, "demandes");
            return ResponseEntity.ok(d);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }
}