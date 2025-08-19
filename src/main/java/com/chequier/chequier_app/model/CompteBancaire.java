package com.chequier.chequier_app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comptes_bancaires")
@Getter @Setter
public class CompteBancaire {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // ⬇️ IMPORTANT : on mappe typeCompte sur la colonne 'banque'
  @NotBlank
  @Column(name = "banque", length = 50)
  private String typeCompte; // ex: "COURANT", "EPARGNE"...

  @NotBlank
  @Column(name = "numero_compte", length = 50)
  private String numeroCompte;

  @Column(length = 20)
  private String rib; // optionnel

  @Column(length = 34)
  private String iban; // optionnel

  @Column(length = 10)
  private String devise; // ex: "TND", "EUR"

  @Column(name = "is_default", nullable = false)
  private boolean isDefault = false;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;
}
