package com.chequier.chequier_app.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chequier.chequier_app.model.CompteBancaire;
import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.CompteBancaireRepository;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;

import jakarta.validation.constraints.NotBlank;

@Service
public class CompteService {

    private final CompteBancaireRepository compteRepo;
    private final UserRepository userRepo;
    private final HistoriqueRepository histoRepo;

    public CompteService(CompteBancaireRepository compteRepo,
                         UserRepository userRepo,
                         HistoriqueRepository histoRepo) {
        this.compteRepo = compteRepo;
        this.userRepo = userRepo;
        this.histoRepo = histoRepo;
    }

    /* ================= DTO internes ================= */

    /** Création d’un compte (entrée) */
    public static class CreateRequest {
        @NotBlank public String typeCompte;      
        @NotBlank public String numeroCompte;    
        public String rib;
        public String iban;
        public String devise;
        public boolean isDefault;                
    }

    /** Mise à jour d’un compte (entrée) */
    public static class UpdateRequest {
        public String typeCompte;
        public String numeroCompte;
        public String rib;
        public String iban;
        public String devise;
        public Boolean isDefault; 
    }

    /** Réponse (sortie) */
    public static class CompteResponse {
        public Long id;
        public String typeCompte;
        public String numeroCompte;
        public String rib;
        public String iban;
        public String devise;
        public boolean isDefault;
        public CompteResponse(CompteBancaire c) {
            this.id = c.getId();
            this.typeCompte = c.getTypeCompte();
            this.numeroCompte = c.getNumeroCompte();
            this.rib = c.getRib();
            this.iban = c.getIban();
            this.devise = c.getDevise();
            this.isDefault = c.isDefault();
        }
    }

    /* ================ Métier ================ */

    private static String t(String s) { 
        if (s == null) return null;
        String x = s.trim();
        return x.isEmpty() ? null : x;
    }

    private User requireUser(Authentication auth) {
        if (auth == null || auth.getName() == null) throw new RuntimeException("Unauthorized");
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
    }

    public List<CompteResponse> list(Authentication auth) {
        User user = requireUser(auth);
        return compteRepo.findByUserId(user.getId()).stream()
                .sorted(Comparator.comparing(CompteBancaire::isDefault).reversed()
                        .thenComparing(CompteBancaire::getId).reversed())
                .map(CompteResponse::new)
                .toList();
    }

    @Transactional
    public CompteResponse create(Authentication auth, CreateRequest in) {
        User user = requireUser(auth);

        String type = t(in.typeCompte);
        String num  = t(in.numeroCompte);
        if (type == null) throw new IllegalArgumentException("Le type de compte est requis");
        if (num == null)  throw new IllegalArgumentException("Le numéro de compte est requis");

        // construire entité
        CompteBancaire c = new CompteBancaire();
        c.setId(null);
        c.setTypeCompte(type);
        c.setNumeroCompte(num);
        c.setRib(t(in.rib));
        c.setIban(t(in.iban));
        c.setDevise(t(in.devise));
        c.setUser(user);

        // si demandé par défaut : enlever l'ancien défaut (atomique via @Transactional)
        if (in.isDefault) {
            compteRepo.findByUserId(user.getId()).forEach(existing -> {
                if (existing.isDefault()) {
                    existing.setDefault(false);
                    compteRepo.save(existing);
                }
            });
            c.setDefault(true);
        }

        CompteBancaire saved = compteRepo.save(c);
        log(user, "CREATE_COMPTE", "Création compte " + saved.getNumeroCompte(), "comptes/create",
            "COMPTE_BANCAIRE", saved.getId(), saved.getNumeroCompte());
        return new CompteResponse(saved);
    }

    @Transactional
    public CompteResponse update(Authentication auth, Long id, UpdateRequest in) {
        User user = requireUser(auth);
        CompteBancaire c = compteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé: " + id));
        if (!c.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé à ce compte");
        }

        if (in.typeCompte != null)   c.setTypeCompte(t(in.typeCompte));
        if (in.numeroCompte != null) c.setNumeroCompte(t(in.numeroCompte));
        if (in.rib != null)          c.setRib(t(in.rib));
        if (in.iban != null)         c.setIban(t(in.iban));
        if (in.devise != null)       c.setDevise(t(in.devise));

        // gérer le “par défaut” si demandé
        if (in.isDefault != null && in.isDefault) {
            compteRepo.findByUserId(user.getId()).forEach(existing -> {
                if (existing.isDefault()) {
                    existing.setDefault(false);
                    compteRepo.save(existing);
                }
            });
            c.setDefault(true);
        }

        CompteBancaire saved = compteRepo.save(c);
        log(user, "UPDATE_COMPTE", "Mise à jour compte " + saved.getNumeroCompte(), "comptes/update",
            "COMPTE_BANCAIRE", saved.getId(), saved.getNumeroCompte());
        return new CompteResponse(saved);
    }

    @Transactional
    public CompteResponse setDefault(Authentication auth, Long id) {
        User user = requireUser(auth);
        CompteBancaire c = compteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé: " + id));
        if (!c.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé à ce compte");
        }

        compteRepo.findByUserId(user.getId()).forEach(existing -> {
            if (existing.isDefault()) {
                existing.setDefault(false);
                compteRepo.save(existing);
            }
        });
        c.setDefault(true);
        CompteBancaire saved = compteRepo.save(c);

        log(user, "SET_DEFAULT_COMPTE", "Compte par défaut: " + saved.getNumeroCompte(), "comptes/setDefault",
            "COMPTE_BANCAIRE", saved.getId(), saved.getNumeroCompte());
        return new CompteResponse(saved);
    }

    @Transactional
    public void delete(Authentication auth, Long id) {
        User user = requireUser(auth);
        CompteBancaire c = compteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé: " + id));
        if (!c.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé à ce compte");
        }
        compteRepo.delete(c);
        log(user, "DELETE_COMPTE", "Suppression compte " + c.getNumeroCompte(), "comptes/delete",
            "COMPTE_BANCAIRE", c.getId(), c.getNumeroCompte());
    }

    /* ============ log util ============ */
    private void log(User acteur, String action, String message, String page,
                     String ressourceType, Long ressourceId, String ressourceLabel) {
        try {
            Historique h = new Historique();
            h.setActeur(acteur);
            h.setActeurEmail(acteur.getEmail());
            h.setActeurRole(acteur.getRole() != null ? acteur.getRole().name() : null);
            h.setAction(action);
            h.setMessage(message);
            h.setPage(page);
            h.setRessourceType(ressourceType);
            h.setRessourceId(ressourceId != null ? String.valueOf(ressourceId) : null);
            h.setRessourceLabel(ressourceLabel);
            histoRepo.save(h);
        } catch (Exception ignore) { }
    }
}
