package com.chequier.chequier_app.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;
import com.chequier.chequier_app.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final HistoriqueRepository historiqueRepo;

  public AuthController(UserRepository users,
                        PasswordEncoder encoder,
                        JwtService jwt,
                        HistoriqueRepository historiqueRepo) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
    this.historiqueRepo = historiqueRepo;
  }

  // -------- utils --------
  private String trimToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private void log(User u, String action, String message, String page) {
    if (u == null || historiqueRepo == null) return;
    try {
      Historique h = new Historique();
    h.setActeur(u);                    
h.setActeurEmail(u.getEmail());    
h.setActeurRole(u.getRole()!=null? u.getRole().name(): null);


      // Optionnels mais utiles
      h.setActeurRole(u.getRole() != null ? u.getRole().name() : null);
      h.setMessage(message);
      h.setPage(page);
      h.setRessourceType("USER");
      h.setRessourceId(String.valueOf(u.getId()));   // colonne VARCHAR
      h.setRessourceLabel(u.getEmail());

      historiqueRepo.save(h);
    } catch (Exception e) {
      System.err.println("Erreur log: " + e.getMessage()); // ne bloque jamais la réponse
    }
  }

  // DTOs
  public static class RegisterBody {
    public String nom;
    public String prenom;
    public String email;
    public String password;
    public Role role;                 // CLIENT (défaut) ou AGENT
    public String numCin;
    public String numCompteBancaire;  // ✅ MODIFIÉ: optionnel pour tous les rôles maintenant
  }

  public static class LoginBody {
    public String email;
    public String password;
  }

  // -------- REGISTER (safe) --------
  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterBody body) {
    System.out.println("DEBUG: Début de register()");
    
    if (body == null) {
      System.out.println("DEBUG: Body est null");
      return ResponseEntity.badRequest().body(Map.of("error", "Corps de requête manquant"));
    }
    
    try {
      System.out.println("DEBUG: Parsing des données...");
      String nom = trimToNull(body.nom);
      String prenom = trimToNull(body.prenom);
      String email = trimToNull(body.email);
      String password = trimToNull(body.password);
      String numCin = trimToNull(body.numCin);
      Role role = (body.role == null ? Role.CLIENT : body.role);
      String numCompteBancaire = trimToNull(body.numCompteBancaire);
      
      System.out.println("DEBUG: Données parsées - role: " + role);

      // Obligatoires
      if (nom == null)      return ResponseEntity.badRequest().body(Map.of("error", "Le nom est obligatoire"));
      if (prenom == null)   return ResponseEntity.badRequest().body(Map.of("error", "Le prénom est obligatoire"));
      if (email == null)    return ResponseEntity.badRequest().body(Map.of("error", "L'email est obligatoire"));
      if (password == null) return ResponseEntity.badRequest().body(Map.of("error", "Le mot de passe est obligatoire"));
      if (numCin == null)   return ResponseEntity.badRequest().body(Map.of("error", "Le CIN est obligatoire"));

      if (!email.contains("@"))
        return ResponseEntity.badRequest().body(Map.of("error", "Format d'email invalide"));
      if (!numCin.matches("^[0-9]{8}$"))
        return ResponseEntity.badRequest().body(Map.of("error", "Le CIN doit contenir exactement 8 chiffres"));

      if (numCompteBancaire != null && !numCompteBancaire.matches("^[0-9]{10,20}$")) {
        return ResponseEntity.badRequest().body(Map.of("error", "Le compte bancaire doit contenir entre 10 et 20 chiffres"));
      }

      if (users.existsByEmail(email))
        return ResponseEntity.badRequest().body(Map.of("error", "Email déjà utilisé"));
      if (users.existsByNumCin(numCin))
        return ResponseEntity.badRequest().body(Map.of("error", "CIN déjà utilisé"));
      
      if (numCompteBancaire != null && users.existsByNumCompteBancaire(numCompteBancaire))
        return ResponseEntity.badRequest().body(Map.of("error", "Compte bancaire déjà utilisé"));

      // Sauvegarde - avec debugging
      if (encoder == null) {
        System.err.println("ERREUR: PasswordEncoder est null");
        return ResponseEntity.internalServerError().body(Map.of("error", "Service d'encodage indisponible"));
      }
      
      System.out.println("DEBUG: Encodage du mot de passe...");
      String encoded = encoder.encode(password);
      System.out.println("DEBUG: Mot de passe encodé");
      
      if (role == null) role = Role.CLIENT; // ceinture + bretelles
      
      System.out.println("DEBUG: Création de l'utilisateur...");
      System.out.println("DEBUG: Paramètres - nom: " + nom + ", prenom: " + prenom + ", email: " + email + ", role: " + role + ", cin: " + numCin + ", compte: " + numCompteBancaire);
      
      User u = new User(nom, prenom, email, encoded, role, numCin, numCompteBancaire);
      System.out.println("DEBUG: Utilisateur créé en mémoire");
      
      System.out.println("DEBUG: Sauvegarde en base...");
      u = users.save(u); // Récupérer l'utilisateur sauvé avec l'ID généré
      System.out.println("DEBUG: Utilisateur sauvé avec ID: " + u.getId());

      // Token et log - avec vérifications null
      String token = null;
      if (jwt != null) {
        try {
          System.out.println("DEBUG: Génération du token...");
          token = jwt.generateToken(u.getEmail());
          System.out.println("DEBUG: Token généré: " + (token != null ? "OK" : "NULL"));
        } catch (Exception e) {
          System.err.println("Erreur génération token: " + e.getMessage());
          e.printStackTrace();
        }
      }
      
      // Log seulement si l'utilisateur a été sauvé avec succès
      if (u != null && u.getId() != null) {
        try {
          System.out.println("DEBUG: Ajout du log...");
          log(u, "REGISTER", "Inscription réussie", "register");
          System.out.println("DEBUG: Log ajouté");
        } catch (Exception e) {
          System.err.println("Erreur lors du log: " + e.getMessage());
          e.printStackTrace();
        }
      }

      System.out.println("DEBUG: Début construction de la réponse...");

      // Réponse - avec debugging et protection
      System.out.println("DEBUG: Création de la réponse pour utilisateur ID: " + u.getId());
      
      LinkedHashMap<String, Object> userMap = new LinkedHashMap<>();
      
      try {
        userMap.put("id", u.getId());
        System.out.println("DEBUG: ID ajouté: " + u.getId());
        
        userMap.put("nom", u.getNom() != null ? u.getNom() : "");
        System.out.println("DEBUG: Nom ajouté: " + u.getNom());
        
        userMap.put("prenom", u.getPrenom() != null ? u.getPrenom() : "");
        System.out.println("DEBUG: Prénom ajouté: " + u.getPrenom());
        
        userMap.put("email", u.getEmail() != null ? u.getEmail() : "");
        System.out.println("DEBUG: Email ajouté: " + u.getEmail());
        
        String roleStr = "CLIENT";
        if (u.getRole() != null) {
          try {
            roleStr = u.getRole().name();
          } catch (Exception e) {
            System.err.println("Erreur lors de l'accès au rôle: " + e.getMessage());
          }
        }
        userMap.put("role", roleStr);
        System.out.println("DEBUG: Rôle ajouté: " + roleStr);
        
        userMap.put("numCin", u.getNumCin() != null ? u.getNumCin() : "");
        System.out.println("DEBUG: CIN ajouté: " + u.getNumCin());
        
        if (u.getNumCompteBancaire() != null) {
          userMap.put("numCompteBancaire", u.getNumCompteBancaire());
          System.out.println("DEBUG: Compte bancaire ajouté: " + u.getNumCompteBancaire());
        }
      } catch (Exception e) {
        System.err.println("Erreur lors de la construction de userMap: " + e.getMessage());
        e.printStackTrace();
        throw e;
      }

      LinkedHashMap<String, Object> resp = new LinkedHashMap<>();
      try {
        resp.put("status", 200);
        resp.put("message", "Utilisateur créé");
        if (token != null && !token.trim().isEmpty()) {
          resp.put("token", token);
        }
        resp.put("user", userMap);
        System.out.println("DEBUG: Réponse construite avec succès");
      } catch (Exception e) {
        System.err.println("Erreur lors de la construction de la réponse: " + e.getMessage());
        e.printStackTrace();
        throw e;
      }

      System.out.println("DEBUG: Envoi de la réponse");
      return ResponseEntity.ok(resp);

    } catch (DataIntegrityViolationException e) {
      return ResponseEntity.badRequest().body(Map.of(
        "error", "Erreur d'intégrité des données",
        "details", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage()
      ));
    } catch (Exception e) {
      e.printStackTrace();
      LinkedHashMap<String, Object> err = new LinkedHashMap<>();
      err.put("error", "Erreur serveur");
      err.put("type", e.getClass().getSimpleName());
      err.put("message", e.getMessage() != null ? e.getMessage() : "Exception sans message");
      err.put("cause", e.getCause() != null ? e.getCause().toString() : "Aucune cause spécifiée");
      return ResponseEntity.internalServerError().body(err);
    }
  }

  // -------- LOGIN (via encoder.matches) --------
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginBody body) {
    if (body == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Corps de requête manquant"));
    }
    
    try {
      String email = trimToNull(body.email);
      String password = trimToNull(body.password);
      
      if (email == null || password == null) {
        return ResponseEntity.status(401).body(Map.of("error", "Email et mot de passe requis"));
      }
      
      User u = users.findByEmail(email).orElseThrow();
      
      if (encoder == null) {
        return ResponseEntity.internalServerError().body(Map.of("error", "Service d'encodage indisponible"));
      }
      
      if (!encoder.matches(password, u.getPassword())) {
        return ResponseEntity.status(401).body(Map.of("error", "Identifiants incorrects"));
      }

      String token = null;
      if (jwt != null) {
        try {
          token = jwt.generateToken(u.getEmail());
        } catch (Exception e) {
          System.err.println("Erreur génération token: " + e.getMessage());
        }
      }

      log(u, "LOGIN", "Connexion réussie", "login");

      LinkedHashMap<String, Object> userMap = new LinkedHashMap<>();
      userMap.put("id", u.getId());
      userMap.put("nom", u.getNom());
      userMap.put("prenom", u.getPrenom());
      userMap.put("email", u.getEmail());
      userMap.put("role", (u.getRole() != null ? u.getRole().name() : "CLIENT"));
      userMap.put("numCin", u.getNumCin());
      if (u.getNumCompteBancaire() != null) {
        userMap.put("numCompteBancaire", u.getNumCompteBancaire());
      }

      LinkedHashMap<String, Object> resp = new LinkedHashMap<>();
      resp.put("status", 200);
      resp.put("message", "Connexion réussie");
      if (token != null) resp.put("token", token);
      resp.put("user", userMap);
      resp.put("role", userMap.get("role"));

      return ResponseEntity.ok(resp);

    } catch (java.util.NoSuchElementException e) {
      return ResponseEntity.status(401).body(Map.of("error", "Identifiants incorrects"));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().body(Map.of(
        "error", "Erreur serveur",
        "message", e.getMessage() != null ? e.getMessage() : "Exception sans message"
      ));
    }
  }

  // -------- LOGOUT --------
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest req) {
    try {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getName() != null) {
        users.findByEmail(auth.getName()).ifPresent(u -> log(u, "LOGOUT", "Déconnexion", "logout"));
      }
      if (req.getSession(false) != null) {
        req.getSession(false).invalidate();
      }
      SecurityContextHolder.clearContext();
      return ResponseEntity.ok(Map.of("message", "Déconnecté"));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.ok(Map.of("message", "Déconnecté (avec erreur)")); // On réussit quand même la déconnexion
    }
  }
}