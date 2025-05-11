package com.adavec.transporte.repository;

import com.adavec.transporte.model.Seguro;
import com.adavec.transporte.model.Unidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UnidadRepository extends JpaRepository<Unidad, Integer> {
    List<Unidad> findByDebisFechaBetween(LocalDate inicio, LocalDate fin);
    Optional<Unidad> findByNoSerie(String noSerie);
    List<Unidad> findByNoSerieEndingWith(String digitosFinales);
    void deleteByNoSerie(String noSerie);


}
