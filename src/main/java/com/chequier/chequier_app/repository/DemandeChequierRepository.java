package com.chequier.chequier_app.repository;

import com.chequier.chequier_app.model.DemandeChequier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DemandeChequierRepository extends JpaRepository<DemandeChequier, Long> {
    List<DemandeChequier> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<DemandeChequier> findByCompte_IdOrderByCreatedAtDesc(Long compteId);
}
