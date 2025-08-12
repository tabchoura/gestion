package com.chequier.chequier_app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chequier.chequier_app.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // ✅ pour vérifier l’unicité à l’inscription
    boolean existsByEmail(String email);

    // ✅ pour vérifier l’unicité lors d’une mise à jour (exclure l’utilisateur courant)
    boolean existsByEmailAndIdNot(String email, Long id);
}
