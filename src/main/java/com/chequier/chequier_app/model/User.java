package com.chequier.chequier_app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
  name = "users",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_email",  columnNames = "email"),
    @UniqueConstraint(name = "uk_user_cin",    columnNames = "num_cin"),
    @UniqueConstraint(name = "uk_user_compte", columnNames = "num_compte_bancaire")
  }
)
@Getter
@Setter
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nom;

  @Column(nullable = false)
  private String prenom;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.CLIENT;

  @Column(name = "num_cin", nullable = false, length = 8, unique = true)
  private String numCin;

  // ✅ Optionnel pour AGENT (nullable = true) ; unique OK (plusieurs NULL autorisés en MySQL)
  @Column(name = "num_compte_bancaire", nullable = true, length = 20, unique = true)
  private String numCompteBancaire;

  // Constructeur simplifié
  public User(String nom, String prenom, String email, String password, Role role) {
    this.nom = nom;
    this.prenom = prenom;
    this.email = email;
    this.password = password;
    this.role = role;
  }

  // Constructeur complet
  public User(String nom, String prenom, String email, String password, Role role,
              String numCin, String numCompteBancaire) {
    this.nom = nom;
    this.prenom = prenom;
    this.email = email;
    this.password = password;
    this.role = role;
    this.numCin = numCin;
    this.numCompteBancaire = numCompteBancaire; // peut être null si AGENT
  }

  // Helpers optionnels
  public boolean isValidCin() {
    return numCin != null && numCin.matches("^[0-9]{8}$");
  }

  public boolean isValidCompteBancaire() {
    return numCompteBancaire != null && numCompteBancaire.matches("^[0-9]{10,20}$");
  }
}
