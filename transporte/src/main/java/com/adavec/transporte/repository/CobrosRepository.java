package com.adavec.transporte.repository;

import com.adavec.transporte.model.Cobros;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CobrosRepository extends JpaRepository<Cobros, Integer> {
    List<Cobros> findByUnidadId(Integer unidadId);
}
