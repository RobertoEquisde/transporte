package com.adavec.transporte.service;

import com.adavec.transporte.model.User;
import com.adavec.transporte.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // Esta es la interfaz de Spring Security
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Buscar usuario por username o email
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con username o email: " + usernameOrEmail));

        // Verificar si el usuario está activo
        if (!user.isActive()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + usernameOrEmail);
        }

        return createUserPrincipal(user);
    }

    // Método para buscar usuario por ID (útil para el JWT)
    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("Usuario inactivo con ID: " + id);
        }

        return createUserPrincipal(user);
    }

    private UserDetails createUserPrincipal(User user) {
        Collection<GrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority(user.getRole().name()) // ✅ Sin concatenar "ROLE_"
        );

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!user.isActive())
                .build();
    }
}