package com.adavec.transporte.service;

import com.adavec.transporte.dto.CrearModeloRequest;
import com.adavec.transporte.model.Modelo;
import com.adavec.transporte.repository.ModeloRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ModeloService {

    private final ModeloRepository modeloRepository;

    public ModeloService(ModeloRepository modeloRepository) {
        this.modeloRepository = modeloRepository;
    }


    public boolean existePorId(Integer id) {
        return modeloRepository.existsById(id);
    }


    public List<Modelo> obtenerTodos() {
        return modeloRepository.findAll();
    }

    public Optional<Modelo> obtenerPorId(Integer id) {
        return modeloRepository.findById(id);
    }

    public Modelo guardar(Modelo modelo) {
        return modeloRepository.save(modelo);
    }
    public Modelo crearModelo(CrearModeloRequest request) {
        Modelo modelo = new Modelo();
        modelo.setNombre(request.getNombre());
        modelo.setUso(request.getUso());
        return modeloRepository.save(modelo);
    }
    @Transactional
    public void eliminarModelo(Integer id) {
        if (!modeloRepository.existsById(id)) {
            throw new RuntimeException("No existe un modelo con ID: " + id);
        }
        modeloRepository.deleteById(id);
    }
}
