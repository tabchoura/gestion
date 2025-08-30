package com.chequier.chequier_app.service;

import com.chequier.chequier_app.model.CompteBancaire;
import com.chequier.chequier_app.model.DemandeChequier;
import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.CompteBancaireRepository;
import com.chequier.chequier_app.repository.DemandeChequierRepository;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoriqueService {

    private final HistoriqueRepository historiqueRepository;
    private final UserRepository userRepository;
    private final DemandeChequierRepository demandeRepository;
    private final CompteBancaireRepository compteRepository;

    public HistoriqueService(HistoriqueRepository historiqueRepository,
                             UserRepository userRepository,
                             DemandeChequierRepository demandeRepository,
                             CompteBancaireRepository compteRepository) {
        this.historiqueRepository = historiqueRepository;
        this.userRepository = userRepository;
        this.demandeRepository = demandeRepository;
        this.compteRepository = compteRepository;
    }

    /* ============== DTO de sortie ============== */
    public static class HistoriqueResponse {
        public Long id;
        public String action;
        public String message;
        public String page;
        public String acteurEmail;
        public String acteurRole;
        public String ressourceType;
        public String ressourceId;
        public String ressourceLabel;
        public LocalDateTime creeLe; // adapte le type selon ton champ

        public HistoriqueResponse(Historique h) {
            this.id = h.getId();
            this.action = h.getAction();
            this.message = h.getMessage();
            this.page = h.getPage();
            this.acteurEmail = h.getActeurEmail();
            this.acteurRole = h.getActeurRole();
            this.ressourceType = h.getRessourceType();
            this.ressourceId = h.getRessourceId();
            this.ressourceLabel = h.getRessourceLabel();
            this.creeLe = h.getCreeLe(); // si ton champ s’appelle différemment, ajuste
        }
    }

    /* ============== Utils ============== */

    private User requireUser(String email) {
        if (email == null) throw new RuntimeException("Unauthorized");
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Unauthorized"));
    }

    /** Écrit un log dans une NOUVELLE transaction. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User user, String action, String message, String page) {
        Historique h = new Historique();
        h.setAction(action);
        h.setMessage(message);
        h.setPage(page);
        h.setActeur(user);
        h.setActeurEmail(user != null ? user.getEmail() : null);
        h.setActeurRole(user != null && user.getRole()!=null ? user.getRole().name() : null);
        historiqueRepository.save(h);
    }

    /* ============== Lecture (readOnly) ============== */

    /** L’utilisateur voit ses 100 derniers logs. */
    @Transactional(readOnly = true)
    public List<HistoriqueResponse> getUserHistory(@NotNull String email) {
        User user = requireUser(email);
        return historiqueRepository
                .findTop100ByActeur_IdOrderByCreeLeDesc(user.getId())
                .stream().map(HistoriqueResponse::new).toList();
    }

    /** Historique d’une demande — Agent = tout, sinon propriétaire de la demande. */
    @Transactional(readOnly = true)
    public List<HistoriqueResponse> getHistoryByDemande(@NotNull String email, @NotNull Long demandeId) {
        User user = requireUser(email);
        DemandeChequier d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));

        if (user.getRole() != Role.AGENT && !d.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé à cette demande");
        }

        return historiqueRepository.findByDemande_IdOrderByCreeLeDesc(demandeId)
                .stream().map(HistoriqueResponse::new).toList();
    }

    /** Historique d’un compte — Agent = tout, sinon propriétaire du compte. */
    @Transactional(readOnly = true)
    public List<HistoriqueResponse> getHistoryByCompte(@NotNull String email, @NotNull Long compteId) {
        User user = requireUser(email);
        CompteBancaire c = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        if (user.getRole() != Role.AGENT && !c.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé à ce compte");
        }

        return historiqueRepository.findByCompte_IdOrderByCreeLeDesc(compteId)
                .stream().map(HistoriqueResponse::new).toList();
    }
}
