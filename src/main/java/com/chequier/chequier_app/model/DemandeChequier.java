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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "demande_chequier")
@Getter
@Setter
@NoArgsConstructor
public class DemandeChequier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id", nullable = false)
    private CompteBancaire compte;

    @ManyToOne(fetch = FetchType.LAZY)
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
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public DemandeChequier(CompteBancaire compte, User user,
                           LocalDate dateDemande, Integer pages, String motif) {
        this.compte = compte;
        this.user = user;
        this.dateDemande = dateDemande;
        this.pages = pages;
        this.motif = motif;
    }
}
