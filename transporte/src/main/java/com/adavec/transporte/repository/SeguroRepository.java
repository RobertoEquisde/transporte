package com.adavec.transporte.repository;

import com.adavec.transporte.model.Seguro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeguroRepository extends JpaRepository<Seguro, Integer> {
    Seguro findByUnidadId(Integer unidadId);
    List<Seguro> findByFacturaContainingIgnoreCase(String factura);
    void deleteByFactura(String factura);
    boolean existsByFactura(String factura);


}
