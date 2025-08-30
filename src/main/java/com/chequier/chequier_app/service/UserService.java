package com.chequier.chequier_app.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;
import com.chequier.chequier_app.security.JwtService;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final HistoriqueRepository historiqueRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       HistoriqueRepository historiqueRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.historiqueRepository = historiqueRepository;
    }

    /* ==================== DTOs internes (request/response) ==================== */

    /** DTO d'entrée: inscription */
    public static class RegisterBody {
        @NotBlank public String nom;
        @NotBlank public String prenom;
        @Email @NotBlank public String email;
        @NotBlank public String password;
        public Role role; 
        @Pattern(regexp="^[0-9]{8}$", message="CIN invalide (8 chiffres)")
        public String numCin;
        @Pattern(regexp="^[0-9]{10,20}$", message="Compte bancaire invalide (10 à 20 chiffres)")
        public String numCompteBancaire;
    }

    /** DTO d'entrée: login */
    public static class LoginBody {
        @Email @NotBlank public String email;
        @NotBlank public String password;
    }

    /** DTO d'entrée: mise à jour de profil */
    public static class UpdateProfileRequest {
        public String nom;
        public String prenom;
        @Email public String email;
        public String currentPassword;
        public String newPassword;
        @Pattern(regexp="^[0-9]{8}$", message="CIN invalide (8 chiffres)")
        public String numCin;
        @Pattern(regexp="^[0-9]{10,20}$", message="Compte bancaire invalide (10 à 20 chiffres)")
        public String numCompteBancaire;
    }

    /** DTO de sortie: utilisateur */
    public static class UserResponse {
        public Long id;
        public String nom;
        public String prenom;
        public String email;
        public String role;
        public String numCin;
        public String numCompteBancaire;

        public UserResponse(User u) {
            this.id = u.getId();
            this.nom = u.getNom();
            this.prenom = u.getPrenom();
            this.email = u.getEmail();
            this.role = (u.getRole()!=null ? u.getRole().name() : null);
            this.numCin = u.getNumCin();
            this.numCompteBancaire = u.getNumCompteBancaire();
        }
    }

    /** DTO de sortie: auth (message + token + user) */
    public static class AuthResponse {
        public String message;
        public String token;
        public UserResponse user;

        public AuthResponse(String message, String token, User u) {
            this.message = message;
            this.token = token;
            this.user = new UserResponse(u);
        }
    }

    /* ==================== Méthodes métier ==================== */

    @Transactional
    public AuthResponse register(RegisterBody in) {
        // Rôles & validations métier
        Role role = (in.role == null ? Role.CLIENT : in.role);

        if (userRepository.existsByEmail(in.email))
            throw new IllegalArgumentException("Email déjà utilisé");
        if (userRepository.existsByNumCin(in.numCin))
            throw new IllegalArgumentException("CIN déjà utilisé");
        if (in.numCompteBancaire != null &&
            userRepository.existsByNumCompteBancaire(in.numCompteBancaire))
            throw new IllegalArgumentException("Compte bancaire déjà utilisé");

        // Créer & persister l'entité
        User user = new User(in.nom, in.prenom, in.email,
                passwordEncoder.encode(in.password),
                role, in.numCin, in.numCompteBancaire);
        user = userRepository.save(user);

        // JWT + log
        String token = (jwtService != null) ? jwtService.generateToken(user.getEmail()) : null;
        log(user, "REGISTER", "Inscription réussie", "register");

        return new AuthResponse("Utilisateur créé", token, user);
    }

    public AuthResponse login(LoginBody in) {
        User user = userRepository.findByEmail(in.email)
                .orElseThrow(() -> new IllegalArgumentException("Identifiants incorrects"));
        if (!passwordEncoder.matches(in.password, user.getPassword())) {
            throw new IllegalArgumentException("Identifiants incorrects");
        }
        String token = (jwtService != null) ? jwtService.generateToken(user.getEmail()) : null;
        log(user, "LOGIN", "Connexion réussie", "login");
        return new AuthResponse("Connexion réussie", token, user);
    }

    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(u -> log(u, "LOGOUT", "Déconnexion", "logout"));
    }

    public UserResponse me(Authentication auth) {
        if (auth == null || auth.getName() == null)
            throw new RuntimeException("Unauthorized");
        User me = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        return new UserResponse(me);
    }

    @Transactional
    public UserResponse updateProfile(Authentication auth, UpdateProfileRequest in) {
        if (auth == null || auth.getName() == null)
            throw new RuntimeException("Unauthorized");

        User me = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        boolean changed = false;

        
        if (in.nom != null && !in.nom.equals(me.getNom())) { me.setNom(in.nom); changed = true; }
        if (in.prenom != null && !in.prenom.equals(me.getPrenom())) { me.setPrenom(in.prenom); changed = true; }

        if (in.email != null && !in.email.equals(me.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(in.email, me.getId()))
                throw new IllegalArgumentException("Email déjà utilisé");
            me.setEmail(in.email); changed = true;
        }

        if (in.numCin != null && !in.numCin.equals(me.getNumCin())) {
            if (userRepository.existsByNumCinAndIdNot(in.numCin, me.getId()))
                throw new IllegalArgumentException("CIN déjà utilisé");
            me.setNumCin(in.numCin); changed = true;
        }

        if (in.numCompteBancaire != null && !in.numCompteBancaire.equals(me.getNumCompteBancaire())) {
            if (userRepository.existsByNumCompteBancaireAndIdNot(in.numCompteBancaire, me.getId()))
                throw new IllegalArgumentException("Compte bancaire déjà utilisé");
            me.setNumCompteBancaire(in.numCompteBancaire); changed = true;
        }

        if (in.newPassword != null) {
            if (in.currentPassword == null || !passwordEncoder.matches(in.currentPassword, me.getPassword()))
                throw new IllegalArgumentException("Mot de passe actuel invalide");
            me.setPassword(passwordEncoder.encode(in.newPassword)); changed = true;
        }

        if (changed) {
            userRepository.save(me);
            log(me, "UPDATE_PROFILE", "Profil mis à jour", "profile");
        }

        return new UserResponse(me);
    }

    /* ==================== Utils ==================== */

    private void log(User u, String action, String message, String page) {
        if (u == null || historiqueRepository == null) return;
        try {
            Historique h = new Historique();
            h.setAction(action);
            h.setMessage(message);
            h.setPage(page);
            h.setActeur(u);
            h.setActeurEmail(u.getEmail());
            h.setActeurRole(u.getRole() != null ? u.getRole().name() : null);
            h.setRessourceType("USER");
            h.setRessourceId(String.valueOf(u.getId()));
            h.setRessourceLabel(u.getEmail());
            historiqueRepository.save(h);
        } catch (Exception ignored) { }
    }
}
