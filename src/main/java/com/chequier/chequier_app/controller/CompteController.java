package com.chequier.chequier_app.controller;

import com.chequier.chequier_app.model.CompteBancaire;
import com.chequier.chequier_app.model.User;
import com.chequier.chequier_app.repository.CompteBancaireRepository;
import com.chequier.chequier_app.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comptes")
@CrossOrigin
public class CompteController {
  private final CompteBancaireRepository comptes;
  private final UserRepository users;

  public CompteController(CompteBancaireRepository comptes, UserRepository users) {
    this.comptes = comptes; this.users = users;
  }

  private User current(Authentication auth){
    return users.findByEmail(auth.getName()).orElseThrow();
  }

  private Object sanitize(CompteBancaire c){
    return new Object(){
      public final Long id = c.getId();
      public final String banque = c.getBanque();
      public final String titulaire = c.getTitulaire();
      public final String numeroCompte = c.getNumeroCompte();
      public final String rib = c.getRib();
      public final String iban = c.getIban();
      public final String devise = c.getDevise();
      public final boolean isDefault = c.isDefault();
    };
  }

  // ✅ Compatible /comptes ET /comptes/mine (ton front appelle /mine)
  @GetMapping({"", "/mine"})
  public List<?> list(Authentication auth){
    var u = current(auth);
    return comptes.findByUserId(u.getId()).stream().map(this::sanitize).toList();
  }

  @PostMapping
  public Object create(Authentication auth, @Valid @RequestBody CompteBancaire req){
    var u = current(auth);
    req.setId(null);
    req.setUser(u);
    if (req.isDefault()) {
      comptes.findByUserId(u.getId()).forEach(c -> { if (c.isDefault()) { c.setDefault(false); comptes.save(c); }});
    }
    var saved = comptes.save(req);
    return sanitize(saved);
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(Authentication auth, @PathVariable Long id, @RequestBody CompteBancaire body){
    var u = current(auth);
    var c = comptes.findById(id).orElse(null);
    if (c == null || !c.getUser().getId().equals(u.getId())) return ResponseEntity.notFound().build();

    if (body.getBanque()!=null) c.setBanque(body.getBanque());
    if (body.getTitulaire()!=null) c.setTitulaire(body.getTitulaire());
    if (body.getNumeroCompte()!=null) c.setNumeroCompte(body.getNumeroCompte());
    if (body.getRib()!=null) c.setRib(body.getRib());
    if (body.getIban()!=null) c.setIban(body.getIban());
    if (body.getDevise()!=null) c.setDevise(body.getDevise());
    if (body.isDefault()) {
      comptes.findByUserId(u.getId()).forEach(x -> { if (x.isDefault()) { x.setDefault(false); comptes.save(x); }});
      c.setDefault(true);
    }
    return ResponseEntity.ok(sanitize(comptes.save(c)));
  }

  @PutMapping("/{id}/default")
  public ResponseEntity<?> setDefault(Authentication auth, @PathVariable Long id){
    var u = current(auth);
    var c = comptes.findById(id).orElse(null);
    if (c == null || !c.getUser().getId().equals(u.getId())) return ResponseEntity.notFound().build();

    comptes.findByUserId(u.getId()).forEach(x -> { if (x.isDefault()) { x.setDefault(false); comptes.save(x); }});
    c.setDefault(true);
    return ResponseEntity.ok(sanitize(comptes.save(c)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id){
    var u = current(auth);
    var c = comptes.findById(id).orElse(null);
    if (c == null || !c.getUser().getId().equals(u.getId())) return ResponseEntity.notFound().build();
    comptes.delete(c);
    return ResponseEntity.noContent().build();
  }
}