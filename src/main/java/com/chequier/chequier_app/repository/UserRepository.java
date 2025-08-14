package com.chequier.chequier_app.repository;

import com.chequier.chequier_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);                 // ✅ pour AuthController.register()
  boolean existsByEmailAndIdNot(String email, Long id);
}
