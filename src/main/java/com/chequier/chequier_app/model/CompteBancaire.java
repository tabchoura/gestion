package com.chequier.chequier_app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

  @NotBlank
  @Column(name = "banque", length = 50)
  private String typeCompte; 

  //la valeur ne peut pas etre vide 
  @NotBlank
  @Column(name = "numero_compte", length = 50)
  private String numeroCompte;

  @Column(length = 20)
  private String rib; 

  @Column(length = 34)
  private String iban; 

  @Column(length = 10)
  private String devise; 

  @Column(name = "is_default", nullable = false)
  private boolean isDefault = false;
//un client peut avoir plusieurs comptes bancaires 
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;
}
