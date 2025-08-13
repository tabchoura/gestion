package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Historique.RessourceType;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/historique")
@CrossOrigin(origins = "http://localhost:4200")
public class HistoriqueController {

  private final HistoriqueRepository repo;

  public HistoriqueController(HistoriqueRepository repo) {
    this.repo = repo;
  }

  /** Mes 100 derniers événements (acteur = utilisateur courant) */
  @GetMapping
  public List<Map<String,Object>> me(Authentication auth) {
    String email = auth.getName();
    return repo.findTop100ByActeurEmailOrderByCreeLeDesc(email)
               .stream().map(this::toMap).toList();
  }

  /** Historique d'une ressource précise (ex: type=DEMANDE_CHEQUIER&id=42) */
  @GetMapping("/resource")
  public List<Map<String,Object>> byResource(@RequestParam("type") RessourceType type,
                                             @RequestParam("id") Long id) {
    return repo.findByRessourceTypeAndRessourceIdOrderByCreeLeDesc(type, id)
               .stream().map(this::toMap).toList();
  }

  /** Historique global d'un type de ressource (ex: COMPTE) */
  @GetMapping("/type/{type}")
  public List<Map<String,Object>> byType(@PathVariable("type") RessourceType type) {
    return repo.findTop200ByRessourceTypeOrderByCreeLeDesc(type)
               .stream().map(this::toMap).toList();
  }

  // ---- mapping entité -> Map (pas de DTO) ----
  private Map<String,Object> toMap(Historique h) {
    return Map.of(
      "id", h.getId(),
      "acteurEmail", h.getActeurEmail(),
      "acteurRole", h.getActeurRole(),
      "ressourceType", String.valueOf(h.getRessourceType()),
      "ressourceId", h.getRessourceId(),
      "ressourceLabel", h.getRessourceLabel(),
      "action", String.valueOf(h.getAction()),
      "message", h.getMessage(),
      "payloadJson", h.getPayloadJson(),
      "creeLe", h.getCreeLe()
    );
  }
}
