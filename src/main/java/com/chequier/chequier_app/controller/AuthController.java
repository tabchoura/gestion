package com.chequier.chequier_app.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.service.UserService;
import com.chequier.chequier_app.service.UserService.AuthResponse;
import com.chequier.chequier_app.service.UserService.LoginBody;
import com.chequier.chequier_app.service.UserService.RegisterBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

    private final UserService userService;
    public AuthController(UserService userService) { this.userService = userService; }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterBody body) {
        var out = userService.register(body);
        return ResponseEntity.status(201).body(out); // 201 Created
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginBody body) {
        var out = userService.login(body);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null ? auth.getName() : null);

        if (req.getSession(false) != null) req.getSession(false).invalidate();
        SecurityContextHolder.clearContext();

        if (email != null) userService.logout(email);
        return ResponseEntity.ok(Map.of("message", "Déconnecté"));
    }
}
