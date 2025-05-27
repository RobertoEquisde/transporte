package com.adavec.transporte.repository;

import com.adavec.transporte.model.Modelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModeloRepository extends JpaRepository<Modelo, Integer> {
    Optional<Modelo> findByNombre(String nombre);
    List<Modelo> findByNombreContainingIgnoreCase(String nombre);
}
