package com.bloodlink.controller;

import com.bloodlink.dto.AuthResponse;
import com.bloodlink.dto.LoginRequest;
import com.bloodlink.dto.RegisterRequest;
import com.bloodlink.entity.User;
import com.bloodlink.exception.BusinessRuleException;
import com.bloodlink.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login and logout")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── POST /api/auth/register ─────────────────────────────────────────────
    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req) {

        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessRuleException(
                    "Username '" + req.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessRuleException(
                    "Email '" + req.getEmail() + "' is already registered.");
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .build();

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.builder()
                        .message("Registration successful! Please login.")
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build()
        );
    }

    // ── POST /api/auth/login ────────────────────────────────────────────────
    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.getUsername(), req.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(auth);

            // Create session
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT",
                    SecurityContextHolder.getContext());

            User user = userRepository.findByUsername(req.getUsername())
                    .orElseThrow();

            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .message("Login successful!")
                            .username(user.getUsername())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build()
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthResponse.builder()
                            .message("Invalid username or password.")
                            .build()
            );
        }
    }

    // ── GET /api/auth/me ────────────────────────────────────────────────────
    @GetMapping("/me")
    @Operation(summary = "Get the currently logged-in user")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthResponse.builder()
                            .message("Not authenticated")
                            .build()
            );
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthResponse.builder()
                            .message("User not found")
                            .build()
            );
        }

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .message("Authenticated")
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build()
        );
    }
}
