package com.chequier.chequier_app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.service.CompteService;
import com.chequier.chequier_app.service.CompteService.CompteResponse;
import com.chequier.chequier_app.service.CompteService.CreateRequest;
import com.chequier.chequier_app.service.CompteService.UpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/comptes")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class CompteController {

    private final CompteService compteService;
    public CompteController(CompteService compteService) { this.compteService = compteService; }

    @GetMapping({"/mine"})
    public ResponseEntity<List<CompteResponse>> list(Authentication auth) {
        var out = compteService.list(auth);
        return ResponseEntity.ok(out);
    }

    @PostMapping
    public ResponseEntity<CompteResponse> create(Authentication auth,
                                                 @Valid @RequestBody CreateRequest req) {
        var out = compteService.create(auth, req);
        return ResponseEntity.status(201).body(out);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompteResponse> update(Authentication auth,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody UpdateRequest req) {
        var out = compteService.update(auth, id, req);
        return ResponseEntity.ok(out);
    }

    // @PutMapping("/{id}/default")
    // public ResponseEntity<CompteResponse> setDefault(Authentication auth, @PathVariable Long id) {
    //     var out = compteService.setDefault(auth, id);
    //     return ResponseEntity.ok(out);
    // }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        compteService.delete(auth, id);
        return ResponseEntity.noContent().build();
    }
}
