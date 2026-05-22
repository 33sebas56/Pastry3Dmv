package com.backend.pastry3d.auth.controller;

import com.backend.pastry3d.auth.dto.AuthResponse;
import com.backend.pastry3d.auth.dto.LoginRequest;
import com.backend.pastry3d.auth.dto.MeResponse;
import com.backend.pastry3d.auth.dto.RegisterRequest;
import com.backend.pastry3d.auth.service.AuthService;
import com.backend.pastry3d.shared.dto.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request, authenticationManager);
    }

    @GetMapping("/confirm")
    public MessageResponse confirm(@RequestParam String token) {
        authService.confirmEmail(token);
        return new MessageResponse("Cuenta confirmada correctamente");
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
