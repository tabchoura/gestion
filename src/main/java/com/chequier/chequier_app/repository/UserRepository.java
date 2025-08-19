package com.chequier.chequier_app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chequier.chequier_app.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Recherche par email
    Optional<User> findByEmail(String email);

    // Vérifie l’existence globale
    boolean existsByEmail(String email);
    boolean existsByNumCin(String numCin);
    boolean existsByNumCompteBancaire(String numCompteBancaire);

    // Vérifie l’existence pour un autre utilisateur (update)
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByNumCinAndIdNot(String numCin, Long id);
    boolean existsByNumCompteBancaireAndIdNot(String numCompteBancaire, Long id);

    // Recherches optionnelles
    Optional<User> findByNumCin(String numCin);
    Optional<User> findByNumCompteBancaire(String numCompteBancaire);
}
