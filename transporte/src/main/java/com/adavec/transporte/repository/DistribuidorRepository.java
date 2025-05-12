package com.adavec.transporte.repository;

import com.adavec.transporte.model.Distribuidor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistribuidorRepository extends JpaRepository<Distribuidor, Integer> {
    List<Distribuidor> findByClaveDistribuidoraContainingIgnoreCase(String clave);
    Optional<Distribuidor> findFirstByClaveDistribuidora(String clave);

}
