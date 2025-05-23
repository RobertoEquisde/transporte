package com.adavec.transporte.service;

import com.adavec.transporte.dto.CrearDistribuidorDTO;
import com.adavec.transporte.model.Distribuidor;
import com.adavec.transporte.repository.DistribuidorRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DistribuidorService {

    private final DistribuidorRepository distribuidorRepository;

    public DistribuidorService(DistribuidorRepository distribuidorRepository) {
        this.distribuidorRepository = distribuidorRepository;
    }
    public Optional<Distribuidor> buscarPorClaveExacta(String claveDistribuidora) {
        return distribuidorRepository.findByClaveDistribuidora(claveDistribuidora);
    }
    public List<Distribuidor> obtenerTodos() {
        return distribuidorRepository.findAll();
    }
    public Optional<Distribuidor> obtenerPorId(Integer id) {
        return distribuidorRepository.findById(id);
    }
    public Distribuidor guardar(Distribuidor distribuidor) {
        return distribuidorRepository.save(distribuidor);
    }
    public List<Distribuidor> buscarPorClave(String clave) {return distribuidorRepository.findByClaveDistribuidoraContainingIgnoreCase(clave);}
    @Transactional
    public Distribuidor crear(CrearDistribuidorDTO dto) {
        Distribuidor dist = new Distribuidor();
        dist.setNombreDistribuidora(dto.getNombreDistribuidora());
        dist.setClaveDistribuidora(dto.getClaveDistribuidora());
        dist.setContacto(dto.getContacto());
        dist.setTelefono(dto.getTelefono());
        dist.setExtension(dto.getExtension());
        dist.setCorreo(dto.getCorreo());
        dist.setSucursal(dto.getSucursal());
        return distribuidorRepository.save(dist);
    }

    @Transactional
    public void eliminarPorId(Integer id) {
        if (!distribuidorRepository.existsById(id)) {
            throw new RuntimeException("Distribuidor no encontrado con ID: " + id);
        }
        distribuidorRepository.deleteById(id);
    }

}
