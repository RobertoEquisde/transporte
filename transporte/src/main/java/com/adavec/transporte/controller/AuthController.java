package com.adavec.transporte.controller;

import com.adavec.transporte.dto.*;
import com.adavec.transporte.model.User;
import com.adavec.transporte.repository.UserRepository;
import com.adavec.transporte.security.JwtUtils;
import com.adavec.transporte.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          UserRepository userRepository,
                          JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Autenticar usuario
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Generar JWT
            String jwt = jwtUtils.generateToken(userDetails.getUsername());

            // Obtener información del usuario
            User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Crear respuesta
            AuthResponse authResponse = new AuthResponse(
                    jwt,
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name()
            );

            return ResponseEntity.ok(authResponse);

        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Credenciales inválidas");
            error.put("message", "Username/email o contraseña incorrectos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error de autenticación");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            UserResponse userResponse = userService.registerUser(registerRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Usuario registrado exitosamente");
            response.put("user", userResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error de registro");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("message", "Error inesperado durante el registro");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            String username = authentication.getName();
            UserResponse user = userService.getUserByUsername(username);

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al obtener información del usuario");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // En un sistema JWT stateless, el logout se maneja en el frontend
        // eliminando el token del almacenamiento local
        return ResponseEntity.ok(Map.of("message", "Logout exitoso"));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("valid", true, "username", authentication.getName()));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "message", "Token inválido"));
    }
}