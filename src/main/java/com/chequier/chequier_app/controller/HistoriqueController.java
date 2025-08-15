package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.Historique;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.HistoriqueRepository;
import com.chequier.chequier_app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/historique")
@CrossOrigin(origins = "http://localhost:4200")
public class HistoriqueController {
  private final HistoriqueRepository historiqueRepo;
  private final UserRepository userRepo;

  public HistoriqueController(HistoriqueRepository historiqueRepo, UserRepository userRepo) {
    this.historiqueRepo = historiqueRepo;
    this.userRepo = userRepo;
  }

  @GetMapping("/mine")
  public ResponseEntity<List<Historique>> mine(Authentication auth) {
    User u = userRepo.findByEmail(auth.getName()).orElseThrow();
    return ResponseEntity.ok(historiqueRepo.findTop100ByActeurIdOrderByCreeLeDesc(u.getId()));
  }
}
