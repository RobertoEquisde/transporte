package com.adavec.transporte.service;

import com.adavec.transporte.dto.RegisterRequest;
import com.adavec.transporte.dto.UserResponse;
import com.adavec.transporte.model.User;
import com.adavec.transporte.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse registerUser(RegisterRequest registerRequest) {
        // Validar que el username no exista
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Error: El nombre de usuario ya existe!");
        }

        // Validar que el email no exista
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Error: El email ya está en uso!");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());

        // Asignar rol (por defecto USER, o ADMIN si es el primer usuario)
        if (userRepository.count() == 0) {
            user.setRole(User.Role.ADMIN); // Primer usuario es admin
        } else {
            user.setRole(User.Role.USER);
        }

        User savedUser = userRepository.save(user);
        return convertToUserResponse(savedUser);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));
        return convertToUserResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
        return convertToUserResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getActiveUsers() {
        return userRepository.findByIsActiveTrue().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateUser(Long userId, RegisterRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Actualizar campos
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                throw new RuntimeException("Error: El email ya está en uso!");
            }
            user.setEmail(updateRequest.getEmail());
        }

        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setActive(false);
        userRepository.save(user);
    }

    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setActive(true);
        userRepository.save(user);
    }

    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}