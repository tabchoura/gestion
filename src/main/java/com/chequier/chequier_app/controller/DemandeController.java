package com.chequier.chequier_app.controller;

import java.time.LocalDate;
import java.util.List;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.model.CompteBancaire;
import com.chequier.chequier_app.model.DemandeChequier;
import com.chequier.chequier_app.model.DemandeStatut;
import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.CompteBancaireRepository;
import com.chequier.chequier_app.repository.DemandeChequierRepository;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping(value = "/demandes")
@CrossOrigin(origins = "*")
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
    System.out.println("🔍 Getting current user from auth: " + auth.getName());
    User user = users.findByEmail(auth.getName()).orElseThrow(() -> 
        new RuntimeException("User not found: " + auth.getName()));
    System.out.println("✅ Found user: " + user.getId() + " - " + user.getEmail());
    return user;
  }

  private void log(User u, String action, String message, String page) {
    try {
      Historique h = new Historique();
      h.setAction(action);
      h.setMessage(message);
      h.setPage(page);
      h.setActeurId(u.getId());
      h.setActeurEmail(u.getEmail());
      historiqueRepo.save(h);
      System.out.println("📝 Log saved: " + action + " - " + message);
    } catch (Exception e) {
      System.err.println("❌ Error saving log: " + e.getMessage());
    }
  }

  private Object dto(DemandeChequier d) {
    CompteBancaire c = d.getCompte();
    return new Object() {
      public final Long id = d.getId();
      public final Long compteId = (c != null ? c.getId() : null);
      public final String banque = (c != null ? c.getBanque() : null);
      public final String numeroCompte = (c != null ? c.getNumeroCompte() : null);
      public final String devise = (c != null ? c.getDevise() : null);
      public final LocalDate dateDemande = d.getDateDemande();
      public final Integer pages = d.getPages();
      public final String motif = d.getMotif();
      public final DemandeStatut statut = d.getStatut();
      public final java.time.Instant createdAt = d.getCreatedAt();
      public final java.time.Instant updatedAt = d.getUpdatedAt();
    };
  }

  @GetMapping
  public ResponseEntity<List<?>> list(Authentication auth, @RequestParam(required = false) Long compteId) {
    System.out.println("📋 GET /demandes called");
    var me = current(auth);

    if (compteId != null) {
      var cpt = comptes.findById(compteId).orElse(null);
      if (cpt == null || !cpt.getUser().getId().equals(me.getId())) {
        return ResponseEntity.status(404).build();
      }
      var data = demandes.findByCompte_IdOrderByCreatedAtDesc(compteId)
                         .stream().map(this::dto).toList();
      return ResponseEntity.ok(data);
    }

    var data = demandes.findByUser_IdOrderByCreatedAtDesc(me.getId())
                       .stream().map(this::dto).toList();
    return ResponseEntity.ok(data);
  }

  // ===== CREATE =====
  public static class CreateBody {
    public @NotNull Long compteId;
    public LocalDate dateDemande;
    public @NotNull Integer pages;
    public String motif;
  }

  @PostMapping
  public ResponseEntity<?> create(Authentication auth, @Valid @RequestBody CreateBody body) {
    System.out.println("➕ POST /demandes called");
    var me = current(auth);
    var cpt = comptes.findById(body.compteId).orElse(null);
    if (cpt == null || !cpt.getUser().getId().equals(me.getId())) {
      return ResponseEntity.status(404).body("Compte introuvable");
    }

    var d = new DemandeChequier();
    d.setUser(me);
    d.setCompte(cpt);
    d.setDateDemande(body.dateDemande != null ? body.dateDemande : LocalDate.now());
    d.setPages(body.pages);
    d.setMotif(body.motif);
    d.setStatut(DemandeStatut.EN_ATTENTE);

    d = demandes.save(d);

    log(me, "DEMANDE_CREE", "Demande #" + d.getId(), "demandes");

    return ResponseEntity.status(201).body(dto(d));
  }

  // ===== UPDATE (édition champs quand EN_ATTENTE) =====
  public static class UpdateBody {
    public LocalDate dateDemande;
    public Integer pages;
    public String motif;
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(Authentication auth,
                                  @PathVariable Long id,
                                  @Valid @RequestBody UpdateBody body) {
    System.out.println("🔄 PUT /demandes/" + id + " called");
    System.out.println("🔄 Authentication: " + (auth != null ? auth.getName() : "null"));
    
    try {
      var me = current(auth);
      System.out.println("🔄 Current user: " + me.getId());
      
      var d = demandes.findById(id).orElse(null);
      if (d == null) {
        System.out.println("❌ Demande not found: " + id);
        return ResponseEntity.notFound().build();
      }
      
      System.out.println("🔄 Demande found, owner: " + d.getUser().getId());
      System.out.println("🔄 Current user: " + me.getId());
      
      if (!d.getUser().getId().equals(me.getId())) {
        System.out.println("❌ Access denied");
        return ResponseEntity.notFound().build();
      }
      
      if (d.getStatut() != DemandeStatut.EN_ATTENTE) {
        System.out.println("❌ Status not editable: " + d.getStatut());
        return ResponseEntity.badRequest().body("Seules les demandes EN_ATTENTE peuvent être modifiées");
      }

      System.out.println("🔄 Updating fields...");
      if (body.dateDemande != null) {
        System.out.println("🔄 Updating dateDemande: " + body.dateDemande);
        d.setDateDemande(body.dateDemande);
      }
      if (body.pages != null) {
        System.out.println("🔄 Updating pages: " + body.pages);
        d.setPages(body.pages);
      }
      if (body.motif != null) {
        System.out.println("🔄 Updating motif: " + body.motif);
        d.setMotif(body.motif);
      }

      d = demandes.save(d);
      System.out.println("✅ Demande updated successfully");

      log(me, "DEMANDE_MISE_A_JOUR", "Demande #" + d.getId(), "demandes");

      return ResponseEntity.ok(dto(d));
      
    } catch (Exception e) {
      System.err.println("❌ Error in update: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(500).body("Erreur interne: " + e.getMessage());
    }
  }

  // ===== ANNULER =====
  @PutMapping("/{id}/annuler")
  public ResponseEntity<?> annuler(Authentication auth, @PathVariable Long id) {
    System.out.println("❌ PUT /demandes/" + id + "/annuler called");
    try {
      var me = current(auth);
      var d = demandes.findById(id).orElse(null);
      if (d == null || !d.getUser().getId().equals(me.getId())) {
        return ResponseEntity.notFound().build();
      }
      if (d.getStatut() != DemandeStatut.EN_ATTENTE) {
        return ResponseEntity.badRequest().body("Statut non annulable");
      }

      d.setStatut(DemandeStatut.ANNULEE);
      d = demandes.save(d);

      log(me, "DEMANDE_ANNULEE", "Demande #" + d.getId(), "demandes");

      return ResponseEntity.ok(dto(d));
    } catch (Exception e) {
      System.err.println("❌ Error in annuler: " + e.getMessage());
      return ResponseEntity.status(500).body("Erreur interne: " + e.getMessage());
    }
  }

  // ===== DELETE =====
  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
    System.out.println("🗑️ DELETE /demandes/" + id + " called");
    try {
      var me = current(auth);
      var d = demandes.findById(id).orElse(null);
      if (d == null || !d.getUser().getId().equals(me.getId())) {
        return ResponseEntity.notFound().build();
      }

      demandes.delete(d);

      log(me, "DEMANDE_SUPPRIMEE", "Demande #" + id, "demandes");

      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      System.err.println("❌ Error in delete: " + e.getMessage());
      return ResponseEntity.status(500).body("Erreur interne: " + e.getMessage());
    }
  }
}