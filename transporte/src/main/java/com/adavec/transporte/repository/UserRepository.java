package com.adavec.transporte.repository;

import com.adavec.transporte.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Buscar usuario por username
    Optional<User> findByUsername(String username);

    // Buscar usuario por email
    Optional<User> findByEmail(String email);

    // Buscar usuario por username o email
    @Query("SELECT u FROM User u WHERE u.username = :credential OR u.email = :credential")
    Optional<User> findByUsernameOrEmail(@Param("credential") String credential);

    // Verificar si existe username
    boolean existsByUsername(String username);

    // Verificar si existe email
    boolean existsByEmail(String email);

    // Buscar usuarios activos
    List<User> findByIsActiveTrue();

    // Buscar usuarios por rol
    List<User> findByRole(User.Role role);

    // Buscar usuarios activos por rol
    List<User> findByRoleAndIsActiveTrue(User.Role role);
}