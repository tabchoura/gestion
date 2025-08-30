package com.chequier.chequier_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chequier.chequier_app.model.DemandeChequier;

public interface DemandeChequierRepository extends JpaRepository<DemandeChequier, Long> {
    
    List<DemandeChequier> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<DemandeChequier> findByCompte_IdOrderByCreatedAtDesc(Long compteId);
    List<DemandeChequier> findAllByOrderByCreatedAtDesc();
    
    List<DemandeChequier> findByUser_Id(Long userId);
    List<DemandeChequier> findByCompte_Id(Long compteId);
}