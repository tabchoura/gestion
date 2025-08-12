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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name="users", uniqueConstraints=@UniqueConstraint(name="uk_user_email", columnNames="email"))
@Getter @Setter @NoArgsConstructor
public class User {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;

  @NotBlank private String nom;
  @NotBlank private String prenom;

  @Email @NotBlank @Column(nullable=false) private String email;
  @NotBlank private String password;

  @Enumerated(EnumType.STRING) @Column(nullable=false)
  private Role role = Role.CLIENT;

  public User(String nom,String prenom,String email,String password,Role role){
    this.nom=nom; this.prenom=prenom; this.email=email; this.password=password; this.role=role;
  }
}
