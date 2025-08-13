package com.chequier.chequier_app.repository;

import com.chequier.chequier_app.model.CompteBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompteBancaireRepository extends JpaRepository<CompteBancaire, Long> {
  List<CompteBancaire> findByUserId(Long userId);
}
