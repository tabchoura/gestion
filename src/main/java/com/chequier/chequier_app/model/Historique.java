package com.chequier.chequier_app.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique")
public class Historique {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "acteur_email", length = 120, nullable = false)
  private String acteurEmail;

  @Column(name = "acteur_role", length = 40)
  private String acteurRole;

  @Enumerated(EnumType.STRING)
  @Column(name = "ressource_type", length = 40, nullable = false)
  private RessourceType ressourceType;

  @Column(name = "ressource_id")
  private Long ressourceId;

  @Column(name = "ressource_label", length = 180)
  private String ressourceLabel;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", length = 40, nullable = false)
  private ActionType action;

  @Column(name = "message", length = 512)
  private String message;

  @Lob
  @Column(name = "payload_json")
  private String payloadJson;

  @Column(name = "cree_le", nullable = false)
  private LocalDateTime creeLe = LocalDateTime.now();

  public enum RessourceType { COMPTE, DEMANDE_CHEQUIER, CHEQUIER, CHEQUE, AUTH, AUTRE }
  public enum ActionType    { CREER, MODIFIER, SUPPRIMER, CHANGER_STATUT, ANNULER, APPROUVER, REJETER, CONNEXION, DECONNEXION }

  // getters/setters…
  public Long getId() { return id; }
  public String getActeurEmail() { return acteurEmail; }
  public void setActeurEmail(String acteurEmail) { this.acteurEmail = acteurEmail; }
  public String getActeurRole() { return acteurRole; }
  public void setActeurRole(String acteurRole) { this.acteurRole = acteurRole; }
  public RessourceType getRessourceType() { return ressourceType; }
  public void setRessourceType(RessourceType ressourceType) { this.ressourceType = ressourceType; }
  public Long getRessourceId() { return ressourceId; }
  public void setRessourceId(Long ressourceId) { this.ressourceId = ressourceId; }
  public String getRessourceLabel() { return ressourceLabel; }
  public void setRessourceLabel(String ressourceLabel) { this.ressourceLabel = ressourceLabel; }
  public ActionType getAction() { return action; }
  public void setAction(ActionType action) { this.action = action; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public String getPayloadJson() { return payloadJson; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
  public LocalDateTime getCreeLe() { return creeLe; }
  public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
}
