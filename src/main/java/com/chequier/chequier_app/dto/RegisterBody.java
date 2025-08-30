package com.chequier.chequier_app.dto;

import com.chequier.chequier_app.model.Role;

public class RegisterBody {
  public String nom;
  public String prenom;
  public String email;
  public String password;
  public Role role;                // CLIENT par défaut si null
  public String numCin;
  public String numCompteBancaire; // optionnel
}
