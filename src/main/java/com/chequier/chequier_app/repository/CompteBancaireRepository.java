package com.chequier.chequier_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chequier.chequier_app.model.CompteBancaire;

public interface CompteBancaireRepository extends JpaRepository<CompteBancaire, Long> {
  List<CompteBancaire> findByUserId(Long userId);
}