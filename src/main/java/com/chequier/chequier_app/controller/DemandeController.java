package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.*;
import com.chequier.chequier_app.repository.DemandeChequierRepository;
import com.chequier.chequier_app.repository.UserRepository;
import com.chequier.chequier_app.repository.CompteBancaireRepository;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/demandes")
@CrossOrigin
public class DemandeController {

  private final DemandeChequierRepository demandes;
  private final UserRepository users;
  private final CompteBancaireRepository comptes;

  public DemandeController(DemandeChequierRepository demandes,
                           UserRepository users,
                           CompteBancaireRepository comptes) {
    this.demandes = demandes;
    this.users = users;
    this.comptes = comptes;
  }

  private User current(Authentication auth) {
    return users.findByEmail(auth.getName()).orElseThrow();
  }

  /** DTO de sortie compact, aligné sur ton front */
  private Object dto(DemandeChequier d) {
    var c = d.getCompte();
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

  /**
   * GET /demandes
   * GET /demandes?compteId=123
   */
  @GetMapping
  public ResponseEntity<List<?>> list(Authentication auth, @RequestParam(required = false) Long compteId) {
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

  /** Corps de création */
  public static class CreateBody {
    public @NotNull Long compteId;
    public LocalDate dateDemande;   // optionnel
    public @NotNull Integer pages;  // 10 | 25 | 50
    public String motif;
  }

  @PostMapping
  public ResponseEntity<?> create(Authentication auth, @RequestBody CreateBody body) {
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
    return ResponseEntity.status(201).body(dto(d));
  }

  @PutMapping("/{id}/annuler")
  public ResponseEntity<?> annuler(Authentication auth, @PathVariable Long id) {
    var me = current(auth);
    var d = demandes.findById(id).orElse(null);
    if (d == null || !d.getUser().getId().equals(me.getId())) return ResponseEntity.notFound().build();
    if (d.getStatut() != DemandeStatut.EN_ATTENTE) return ResponseEntity.badRequest().body("Statut non annulable");

    d.setStatut(DemandeStatut.ANNULEE);
    return ResponseEntity.ok(dto(demandes.save(d)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
    var me = current(auth);
    var d = demandes.findById(id).orElse(null);
    if (d == null || !d.getUser().getId().equals(me.getId())) return ResponseEntity.notFound().build();

    demandes.delete(d);
    return ResponseEntity.noContent().build();
  }
}
