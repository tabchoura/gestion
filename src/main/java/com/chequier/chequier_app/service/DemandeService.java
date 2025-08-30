package com.chequier.chequier_app.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chequier.chequier_app.model.DemandeChequier;
import com.chequier.chequier_app.model.DemandeStatut;
import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Role;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.CompteBancaireRepository;
import com.chequier.chequier_app.repository.DemandeChequierRepository;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;

@Service
public class DemandeService {

    private final DemandeChequierRepository demandeRepository;
    private final UserRepository userRepository;
    private final CompteBancaireRepository compteRepository;
    private final HistoriqueRepository historiqueRepository;

    public DemandeService(DemandeChequierRepository demandeRepository,
                          UserRepository userRepository,
                          CompteBancaireRepository compteRepository,
                          HistoriqueRepository historiqueRepository) {
        this.demandeRepository = demandeRepository;
        this.userRepository = userRepository;
        this.compteRepository = compteRepository;
        this.historiqueRepository = historiqueRepository;
    }

    /* ===================== DTO internes ===================== */

    /** Entrée: création de demande */
    public static class CreateRequest {
        public @NotNull Long compteId;
        public @NotNull Integer pages;
        public String motif;
        public LocalDate dateDemande; // optionnel (sinon today)
    }

    /** Entrée: mise à jour de demande */
    public static class UpdateRequest {
        public LocalDate dateDemande; // optionnel
        public Integer pages;         // optionnel
        public String motif;          // optionnel
    }

    /** Sortie: réponse vers le front */
    public static class DemandeResponse {
        public Long id;
        public Long userId;
        public Long compteId;
        public LocalDate dateDemande;
        public Integer pages;
        public String motif;
        public DemandeStatut statut;

        public DemandeResponse(DemandeChequier d) {
            this.id = d.getId();
            this.userId = d.getUser() != null ? d.getUser().getId() : null;
            this.compteId = d.getCompte() != null ? d.getCompte().getId() : null;
            this.dateDemande = d.getDateDemande();
            this.pages = d.getPages();
            this.motif = d.getMotif();
            this.statut = d.getStatut();
        }
    }

    /* ====================== Helpers ====================== */

    private User requireUser(String email) {
        if (email == null) throw new RuntimeException("Unauthorized");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
    }

    private void log(User user, String action, String message, String page) {
        try {
            Historique h = new Historique();
            h.setAction(action);
            h.setMessage(message);
            h.setPage(page);
            h.setActeur(user);
            // (si votre entité Historique a acteurEmail/acteurRole/ressource*, remplissez ici)
            historiqueRepository.save(h);
        } catch (Exception ignored) { }
    }

    /* ====================== Méthodes ====================== */

    @Transactional(readOnly = true)
    public Optional<DemandeResponse> getById(Long id, String userEmail) {
        User user = requireUser(userEmail);

        Optional<DemandeChequier> opt = demandeRepository.findById(id);
        if (opt.isEmpty()) return Optional.empty();

        DemandeChequier d = opt.get();
        if (user.getRole() == Role.AGENT || d.getUser().getId().equals(user.getId())) {
            return Optional.of(new DemandeResponse(d));
        }
        throw new RuntimeException("Accès non autorisé à la demande");
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse> getAll(String userEmail) {
        User user = requireUser(userEmail);
        List<DemandeChequier> list;
        try {
            if (user.getRole() == Role.AGENT) {
                list = demandeRepository.findAllByOrderByCreatedAtDesc();
            } else {
                list = demandeRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des demandes: " + e.getMessage());
            return Collections.emptyList();
        }
        return list.stream().map(DemandeResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse> getMine(String userEmail, Long compteId) {
        User user = requireUser(userEmail);
        try {
            if (compteId != null) {
                return compteRepository.findById(compteId)
                        .filter(c -> c.getUser().getId().equals(user.getId()))
                        .map(c -> demandeRepository.findByCompte_IdOrderByCreatedAtDesc(compteId)
                                .stream().map(DemandeResponse::new).toList())
                        .orElse(Collections.emptyList());
            }
            return demandeRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())
                    .stream().map(DemandeResponse::new).toList();
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des demandes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public DemandeResponse create(String userEmail, CreateRequest in) {
        User user = requireUser(userEmail);

        if (in.pages == null || in.pages <= 0) {
            throw new IllegalArgumentException("Le nombre de pages doit être > 0");
        }

        var compte = compteRepository.findById(in.compteId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Compte introuvable ou non autorisé"));

        DemandeChequier d = new DemandeChequier();
        d.setUser(user);
        d.setCompte(compte);
        d.setDateDemande(in.dateDemande != null ? in.dateDemande : LocalDate.now());
        d.setPages(in.pages);
        d.setMotif(in.motif);
        d.setStatut(DemandeStatut.EN_ATTENTE);

        DemandeChequier saved = demandeRepository.save(d);
        log(user, "DEMANDE_CREE", "Demande #" + saved.getId(), "demandes");
        return new DemandeResponse(saved);
    }

    @Transactional
    public DemandeResponse update(String userEmail, Long id, UpdateRequest in) {
        User user = requireUser(userEmail);

        DemandeChequier d = demandeRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .filter(x -> x.getStatut() == DemandeStatut.EN_ATTENTE)
                .orElseThrow(() -> new RuntimeException("Demande introuvable ou non modifiable"));

        if (in.dateDemande != null) d.setDateDemande(in.dateDemande);
        if (in.pages != null) {
            if (in.pages <= 0) throw new IllegalArgumentException("Le nombre de pages doit être > 0");
            d.setPages(in.pages);
        }
        if (in.motif != null) d.setMotif(in.motif);

        DemandeChequier saved = demandeRepository.save(d);
        log(user, "DEMANDE_MISE_A_JOUR", "Demande #" + saved.getId(), "demandes");
        return new DemandeResponse(saved);
    }

    @Transactional
    public DemandeResponse annuler(String userEmail, Long id) {
        User user = requireUser(userEmail);

        DemandeChequier d = demandeRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .filter(x -> x.getStatut() == DemandeStatut.EN_ATTENTE)
                .orElseThrow(() -> new RuntimeException("Demande introuvable ou non annulable"));

        d.setStatut(DemandeStatut.ANNULEE);
        DemandeChequier saved = demandeRepository.save(d);
        log(user, "DEMANDE_ANNULEE", "Demande #" + saved.getId(), "demandes");
        return new DemandeResponse(saved);
    }

    @Transactional
    public void delete(String userEmail, Long id) {
        User user = requireUser(userEmail);

        if (user.getRole() != Role.AGENT) {
            throw new RuntimeException("Seul un agent peut supprimer une demande");
        }

        DemandeChequier d = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));

        demandeRepository.delete(d);
        log(user, "DEMANDE_SUPPRIMEE", "Demande #" + id, "demandes");
    }

    @Transactional
    public DemandeResponse changeStatut(String userEmail, Long id, DemandeStatut statut) {
        User user = requireUser(userEmail);

        if (user.getRole() != Role.AGENT) {
            throw new RuntimeException("Seul un agent peut modifier le statut");
        }

        DemandeChequier d = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));

        d.setStatut(statut);
        DemandeChequier saved = demandeRepository.save(d);
        log(user, "DEMANDE_STATUT_CHANGE", "Demande #" + id + " → " + statut, "demandes");
        return new DemandeResponse(saved);
    }
}
