package com.chequier.chequier_app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter; import lombok.Setter;

@Entity @Table(name = "comptes_bancaires")
@Getter @Setter
public class CompteBancaire {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank private String banque;
  @NotBlank private String titulaire;
  @NotBlank @Column(length = 50) private String numeroCompte;

  @Column(length = 20) private String rib;   // optionnel (20 chiffres)
  @Column(length = 34) private String iban;  // optionnel
  @Column(length = 10) private String devise;
  private boolean isDefault = false;

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;
}
