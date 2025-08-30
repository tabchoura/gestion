package com.chequier.chequier_app.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chequier.chequier_app.service.UserService;
import com.chequier.chequier_app.service.UserService.UpdateProfileRequest;
import com.chequier.chequier_app.service.UserService.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        var out = userService.me(auth);
        return ResponseEntity.ok(out);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String,Object>> updateProfile(@Valid @RequestBody UpdateProfileRequest req,
                                                            Authentication auth) {
        var userOut = userService.updateProfile(auth, req);
        return ResponseEntity.ok(Map.of(
            "message", "Profil mis à jour avec succès",
            "user", userOut
        ));
    }
}
