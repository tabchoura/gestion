package com.chequier.chequier_app.repository;

import com.chequier.chequier_app.model.Historique;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chequier.chequier_app.model.Historique.RessourceType;
import java.util.List;

public interface HistoriqueRepository extends JpaRepository<Historique, Long> {
  List<Historique> findTop100ByActeurEmailOrderByCreeLeDesc(String email);
  List<Historique> findByRessourceTypeAndRessourceIdOrderByCreeLeDesc(RessourceType type, Long id);
  List<Historique> findTop200ByRessourceTypeOrderByCreeLeDesc(RessourceType type);
}