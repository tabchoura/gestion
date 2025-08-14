package com.chequier.chequier_app.repository;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.Historique.RessourceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoriqueRepository extends JpaRepository<Historique, Long> {

  List<Historique> findByActeurEmailOrderByCreeLeDesc(String email, Pageable pageable);

  List<Historique> findByActeurEmailAndRessourceTypeOrderByCreeLeDesc(
      String email, RessourceType type, Pageable pageable);

  List<Historique> findByActeurEmailAndCreeLeBetweenOrderByCreeLeDesc(
      String email, LocalDateTime debut, LocalDateTime fin, Pageable pageable);
}
