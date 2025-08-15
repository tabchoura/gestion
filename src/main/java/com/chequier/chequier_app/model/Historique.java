package com.chequier.chequier_app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "historique")
public class Historique {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String action;           // ex: LOGIN, DEMANDE_CREE, ...

  @Column(length = 255)
  private String message;          // ex: "Demande #12"

  private String page;             // ex: "login", "demandes", "profil"
  private String ressourceType;    // ex: "DEMANDE"
  private String ressourceLabel;   // ex: "Chéquier courant"
  private String ressourceId;      // ex: "12"

  @Column(nullable = false)
  private Long acteurId;

  @Column(nullable = false)
  private String acteurEmail;

  private String acteurRole;       // optionnel

  @Column(nullable = false)
  private LocalDateTime creeLe;

  @PrePersist
  public void prePersist() { if (creeLe == null) creeLe = LocalDateTime.now(); }

  // Getters/Setters
  // ...
  public Long getId() { return id; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public String getPage() { return page; }
  public void setPage(String page) { this.page = page; }
  public String getRessourceType() { return ressourceType; }
  public void setRessourceType(String ressourceType) { this.ressourceType = ressourceType; }
  public String getRessourceLabel() { return ressourceLabel; }
  public void setRessourceLabel(String ressourceLabel) { this.ressourceLabel = ressourceLabel; }
  public String getRessourceId() { return ressourceId; }
  public void setRessourceId(String ressourceId) { this.ressourceId = ressourceId; }
  public Long getActeurId() { return acteurId; }
  public void setActeurId(Long acteurId) { this.acteurId = acteurId; }
  public String getActeurEmail() { return acteurEmail; }
  public void setActeurEmail(String acteurEmail) { this.acteurEmail = acteurEmail; }
  public String getActeurRole() { return acteurRole; }
  public void setActeurRole(String acteurRole) { this.acteurRole = acteurRole; }
  public LocalDateTime getCreeLe() { return creeLe; }
  public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
}
