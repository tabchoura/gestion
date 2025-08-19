package com.chequier.chequier_app.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "demande_chequier")
public class DemandeChequier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)                 // <— relation
    @JoinColumn(name = "compte_id", nullable = false)
    private CompteBancaire compte;

    @ManyToOne(fetch = FetchType.LAZY)                 // <— relation
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate dateDemande;
    private Integer pages;

    @Column(length = 255)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DemandeStatut statut = DemandeStatut.EN_ATTENTE;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    // --- getters & setters
    public Long getId() { return id; }
    public CompteBancaire getCompte() { return compte; }
    public void setCompte(CompteBancaire compte) { this.compte = compte; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDate dateDemande) { this.dateDemande = dateDemande; }
    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public DemandeStatut getStatut() { return statut; }
    public void setStatut(DemandeStatut statut) { this.statut = statut; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}