package com.chequier.chequier_app.controller;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = {"/api/demandes", "/demandes"}, produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "http://localhost:4200")
public class DemandeController {

  private final DemandeChequierRepository demandes;
  private final UserRepository users;
  private final CompteBancaireRepository comptes;
  private final HistoriqueRepository histo;

  public DemandeController(DemandeChequierRepository demandes,
                           UserRepository users,
                           CompteBancaireRepository comptes,
                           HistoriqueRepository histo) {
    this.demandes = demandes;
    this.users = users;
    this.comptes = comptes;
    this.histo = histo;
  }

  private User current(Authentication auth) {
    return users.findByEmail(auth.getName()).orElseThrow();
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

  private void log(Authentication auth, Historique.RessourceType type, Long resId, String label,
                   Historique.ActionType action, String message, String payloadJson) {
    var h = new Historique();
    h.setActeurEmail(auth.getName());
    h.setActeurRole("CLIENT");
    h.setRessourceType(type);
    h.setRessourceId(resId);
    h.setRessourceLabel(label);
    h.setAction(action);
    h.setMessage(message);
    h.setPayloadJson(payloadJson);
    histo.save(h);
  }

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

  public static class CreateBody {
    public @NotNull Long compteId;
    public LocalDate dateDemande; // optionnel
    public @NotNull Integer pages; // 10 | 25 | 50
    public String motif;
  }

  @PostMapping
  public ResponseEntity<?> create(Authentication auth, @Valid @RequestBody CreateBody body) {
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

    log(auth, Historique.RessourceType.DEMANDE_CHEQUIER, d.getId(), "Demande chéquier",
        Historique.ActionType.CREER, "Création de la demande", null);

    return ResponseEntity.status(201).body(dto(d));
  }

  @PutMapping("/{id}/annuler")
  public ResponseEntity<?> annuler(Authentication auth, @PathVariable Long id) {
    var me = current(auth);
    var d = demandes.findById(id).orElse(null);
    if (d == null || !d.getUser().getId().equals(me.getId())) return ResponseEntity.notFound().build();
    if (d.getStatut() != DemandeStatut.EN_ATTENTE) return ResponseEntity.badRequest().body("Statut non annulable");

    d.setStatut(DemandeStatut.ANNULEE);
    d = demandes.save(d);

    log(auth, Historique.RessourceType.DEMANDE_CHEQUIER, d.getId(), "Demande chéquier",
        Historique.ActionType.ANNULER, "Annulée par le client", "{\"from\":\"EN_ATTENTE\",\"to\":\"ANNULEE\"}");

    return ResponseEntity.ok(dto(d));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
    var me = current(auth);
    var d = demandes.findById(id).orElse(null);
    if (d == null || !d.getUser().getId().equals(me.getId())) return ResponseEntity.notFound().build();

    demandes.delete(d);

    log(auth, Historique.RessourceType.DEMANDE_CHEQUIER, d.getId(), "Demande chéquier",
        Historique.ActionType.SUPPRIMER, "Suppression de la demande", null);

    return ResponseEntity.noContent().build();
  }
}
