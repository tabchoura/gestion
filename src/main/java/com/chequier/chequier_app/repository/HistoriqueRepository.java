package com.chequier.chequier_app.repository;

import com.chequier.chequier_app.model.Historique;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoriqueRepository extends JpaRepository<Historique, Long> {
  List<Historique> findTop100ByActeurIdOrderByCreeLeDesc(Long acteurId);
}
