package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Historique.RessourceType;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/historique", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "http://localhost:4200")
public class HistoriqueController {

  private final HistoriqueRepository repo;
  private final UserRepository users;

  public HistoriqueController(HistoriqueRepository repo, UserRepository users) {
    this.repo = repo;
    this.users = users;
  }

  /** Mon historique complet */
  @GetMapping
  public ResponseEntity<List<Map<String,Object>>> me(Authentication auth,
                                                    @RequestParam(defaultValue = "100") int limit) {
    String email = (auth != null && auth.getName() != null) ? auth.getName() : null;
    if (email == null || email.isBlank()) return ResponseEntity.status(401).body(List.of());

    Pageable pageable = PageRequest.of(0, Math.min(limit, 500));
    var out = repo.findByActeurEmailOrderByCreeLeDesc(email, pageable)
                  .stream().map(this::toMap).toList();
    return ResponseEntity.ok(out);
  }

  /** Historique par type de ressource */
  @GetMapping("/type/{type}")
  public ResponseEntity<List<Map<String,Object>>> byType(Authentication auth,
                                                        @PathVariable String type,
                                                        @RequestParam(defaultValue = "50") int limit) {
    String email = (auth != null && auth.getName() != null) ? auth.getName() : null;
    if (email == null) return ResponseEntity.status(401).body(List.of());

    try {
      RessourceType resourceType = RessourceType.valueOf(type.toUpperCase());
      Pageable pageable = PageRequest.of(0, Math.min(limit, 200));
      var out = repo.findByActeurEmailAndRessourceTypeOrderByCreeLeDesc(email, resourceType, pageable)
                    .stream().map(this::toMap).toList();
      return ResponseEntity.ok(out);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(List.of());
    }
  }

  /** Historique par période */
  @GetMapping("/periode")
  public ResponseEntity<List<Map<String,Object>>> byPeriod(Authentication auth,
                                                          @RequestParam String dateDebut,
                                                          @RequestParam String dateFin,
                                                          @RequestParam(defaultValue = "100") int limit) {
    String email = (auth != null && auth.getName() != null) ? auth.getName() : null;
    if (email == null) return ResponseEntity.status(401).body(List.of());

    try {
      LocalDate debut = LocalDate.parse(dateDebut);
      LocalDate fin = LocalDate.parse(dateFin);
      LocalDateTime dateTimeDebut = LocalDateTime.of(debut, LocalTime.MIN);
      LocalDateTime dateTimeFin = LocalDateTime.of(fin, LocalTime.MAX);

      Pageable pageable = PageRequest.of(0, Math.min(limit, 300));
      var out = repo.findByActeurEmailAndCreeLeBetweenOrderByCreeLeDesc(
                      email, dateTimeDebut, dateTimeFin, pageable)
                    .stream().map(this::toMap).toList();
      return ResponseEntity.ok(out);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(List.of());
    }
  }

  /** ✅ Endpoint TEST : crée 1 ligne d'historique pour l'utilisateur connecté */
  @PostMapping("/test")
  public ResponseEntity<?> addTest(Authentication auth) {
    String email = (auth != null && auth.getName() != null) ? auth.getName() : "unknown";
    User u = users.findByEmail(email).orElse(null);
    String role = u != null ? u.getRole().name() : "UNKNOWN";

    var h = new Historique();
    h.setActeurEmail(email);
    h.setActeurRole(role);
    h.setRessourceType(RessourceType.AUTH);
    h.setAction(Historique.ActionType.CONNEXION);
    h.setMessage("Évènement de test depuis l'interface");
    h.setPayloadJson("{\"test\": true}");
    repo.save(h);
    return ResponseEntity.ok(Map.of("ok", true, "message", "Test ajouté à l'historique"));
  }

  private Map<String,Object> toMap(Historique h) {
    return Map.of(
      "id", h.getId(),
      "acteurEmail", h.getActeurEmail(),
      "acteurRole", h.getActeurRole(),
      "ressourceType", String.valueOf(h.getRessourceType()),
      "ressourceId", (h.getRessourceId() != null ? h.getRessourceId() : 0L),
      "ressourceLabel", h.getRessourceLabel() != null ? h.getRessourceLabel() : "",
      "action", String.valueOf(h.getAction()),
      "message", h.getMessage() != null ? h.getMessage() : "",
      "payloadJson", h.getPayloadJson() != null ? h.getPayloadJson() : "",
      "creeLe", h.getCreeLe()
    );
  }
}
