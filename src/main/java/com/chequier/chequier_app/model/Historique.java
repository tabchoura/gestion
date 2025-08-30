package com.chequier.chequier_app.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "historique")
@Getter @Setter @NoArgsConstructor
public class Historique {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String action;

  @Column(length = 255)
  private String message;

  private String page;
  private String ressourceType;
  private String ressourceLabel;
  private String ressourceId;

  // --- relations ---
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "acteur_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
  private User acteur;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "demande_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
  private DemandeChequier demande;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "compte_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
  private CompteBancaire compte;

  @Column(nullable = false) private String acteurEmail;
  private String acteurRole;

  @Column(nullable = false)
  private LocalDateTime creeLe;

  @PrePersist
  public void prePersist() {
    if (creeLe == null) creeLe = LocalDateTime.now();
    if (acteur != null) {
      if (acteurEmail == null) acteurEmail = acteur.getEmail();
      if (acteurRole == null && acteur.getRole() != null) acteurRole = acteur.getRole().name();
    }
  }

  public Historique(String action, String message, String page,
                    String ressourceType, String ressourceLabel, String ressourceId,
                    User acteur, DemandeChequier demande, CompteBancaire compte) {
    this.action = action;
    this.message = message;
    this.page = page;
    this.ressourceType = ressourceType;
    this.ressourceLabel = ressourceLabel;
    this.ressourceId = ressourceId;
    this.acteur = acteur;
    this.demande = demande;
    this.compte = compte;
  }
}
