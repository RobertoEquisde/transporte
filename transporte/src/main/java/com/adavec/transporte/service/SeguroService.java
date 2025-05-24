package com.adavec.transporte.service;

import com.adavec.transporte.dto.CrearSeguroRequest;
import com.adavec.transporte.dto.SeguroDTO;
import com.adavec.transporte.model.Distribuidor;
import com.adavec.transporte.model.Seguro;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.repository.DistribuidorRepository;
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
    private final DistribuidorRepository distribuidorRepository;
    public SeguroService(SeguroRepository seguroRepository, UnidadRepository unidadRepository, DistribuidorRepository distribuidorRepository) {
        this.seguroRepository = seguroRepository;
        this.unidadRepository = unidadRepository;
        this.distribuidorRepository = distribuidorRepository;
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
    public Seguro actualizarSeguro(SeguroDTO seguroDTO) {
        // 1. Validar existencia del seguro
        Seguro seguro = seguroRepository.findById(seguroDTO.getId())
                .orElseThrow(() -> new RuntimeException("Seguro no encontrado con ID: " + seguroDTO.getId()));

        // 2. Obtener entidades relacionadas
        Unidad unidad = unidadRepository.findById(seguroDTO.getUnidadId())
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada"));

        Distribuidor distribuidora = distribuidorRepository.findById(seguroDTO.getDistribuidorId())
                .orElseThrow(() -> new RuntimeException("Distribuidora no encontrada"));

        // 3. Actualizar todos los campos
        seguro.setFactura(seguroDTO.getFactura());
        seguro.setValorSeguro(seguroDTO.getValorSeguro());
        seguro.setCuotaSeguro(seguroDTO.getCuotaSeguro());
        seguro.setSeguroDistribuidor(seguroDTO.getSeguroDistribuidor());
        seguro.setFechaFactura(seguroDTO.getCuotaFactura());
        seguro.setUnidad(unidad);
        seguro.setDistribuidor(distribuidora); // Usamos el distribuidor del DTO

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

