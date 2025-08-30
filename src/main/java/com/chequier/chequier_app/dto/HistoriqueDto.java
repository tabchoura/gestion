package com.chequier.chequier_app.dto;

import java.time.LocalDateTime;

public class HistoriqueDto {
  public Long id;

  // Acteur
  public Long acteurId;
  public String acteurEmail;
  public String acteurRole;

  // Contexte/trace
  public String message;
  public String page;

  // Ressource générique
  public String ressourceType;
  public String ressourceId;
  public String ressourceLabel;

  // Liens éventuels (si présents dans l'entité)
  public Long demandeId;
  public Long compteId;

  // Horodatage
  public LocalDateTime creeLe;
}
