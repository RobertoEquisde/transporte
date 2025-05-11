package com.adavec.transporte.service;

import com.adavec.transporte.dto.CrearSeguroRequest;
import com.adavec.transporte.model.Seguro;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.repository.SeguroRepository;
import com.adavec.transporte.repository.UnidadRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeguroService {

    private final SeguroRepository seguroRepository;
    private final UnidadRepository unidadRepository;
    public SeguroService(SeguroRepository seguroRepository, UnidadRepository unidadRepository) {
        this.seguroRepository = seguroRepository;
        this.unidadRepository = unidadRepository;
    }
    public List<Seguro> obtenerTodos() {
        return seguroRepository.findAll();
    }

    public Optional<Seguro> obtenerPorId(Integer id) {
        return seguroRepository.findById(id);
    }

    public Seguro guardar(Seguro seguro) {
        return seguroRepository.save(seguro);
    }
    public List<Seguro> buscarPorFactura(String factura) {
        return seguroRepository.findByFacturaContainingIgnoreCase(factura);
    }

    public Seguro guardarSeguroDesdeDTO(CrearSeguroRequest request) {
        Unidad unidad = unidadRepository.findById(request.getUnidadId())
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada"));

        Seguro seguro = new Seguro();
        seguro.setUnidad(unidad);
        seguro.setDistribuidor(unidad.getDistribuidor());
        seguro.setFactura(request.getFactura());
        seguro.setValorSeguro(request.getValorSeguro());
        seguro.setSeguroDistribuidor(request.getSeguroDistribuidor());

        return seguroRepository.save(seguro);
    }
    @Transactional
    public void eliminarPorFactura(String factura) {
        if (!seguroRepository.existsByFactura(factura)) {
            throw new RuntimeException("No se encontró un seguro con la factura: " + factura);
        }
        seguroRepository.deleteByFactura(factura);
    }


}

