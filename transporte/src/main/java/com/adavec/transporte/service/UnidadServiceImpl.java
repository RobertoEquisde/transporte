package com.adavec.transporte.service.impl;

import com.adavec.transporte.dto.UnidadReporteDTO;
import com.adavec.transporte.model.Distribuidor;
import com.adavec.transporte.model.Modelo;
import com.adavec.transporte.model.Seguro;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.repository.DistribuidorRepository;
import com.adavec.transporte.repository.ModeloRepository;
import com.adavec.transporte.repository.SeguroRepository;
import com.adavec.transporte.repository.UnidadRepository;
import com.adavec.transporte.service.UnidadService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UnidadServiceImpl implements UnidadService {

    private final UnidadRepository unidadRepository;
    private final SeguroRepository seguroRepository;
    private DistribuidorRepository distribuidorRepository;
    private ModeloRepository modeloRepository;
    public UnidadServiceImpl(UnidadRepository unidadRepository, SeguroRepository seguroRepository,  ModeloRepository modeloRepository, DistribuidorRepository distribuidorRepository) {
        this.unidadRepository = unidadRepository;
        this.seguroRepository = seguroRepository;
        this.modeloRepository = modeloRepository;
        this.distribuidorRepository = distribuidorRepository;
    }

    @Override
    public List<Unidad> obtenerTodas() {
        return unidadRepository.findAll();
    }

    @Override
    public Optional<Unidad> obtenerPorId(Integer id) {
        return unidadRepository.findById(id);
    }

    @Override
    public Unidad guardar(Unidad unidad) {
        return unidadRepository.save(unidad);
    }


    @Override
    public List<UnidadReporteDTO> obtenerDatosPorMes(YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fin = mes.atEndOfMonth();

        List<Unidad> unidades = unidadRepository.findByDebisFechaBetween(inicio, fin);

        return unidades.stream()
                .map(u -> {
                    Seguro seguro = seguroRepository.findByUnidadId(u.getId());
                    return new UnidadReporteDTO(
                            String.valueOf(u.getId()),
                            u.getDistribuidor() != null ? u.getDistribuidor().getNombreDistribuidora() : "",
                            u.getModelo() != null ? u.getModelo().getNombre() : "",
                            u.getNoSerie(),
                            u.getNoSerie(),
                            u.getDebisFecha() != null ? u.getDebisFecha().toString() : "",
                            u.getModelo() != null ? u.getModelo().getUso() : "",
                            u.getValorUnidad() != null ? u.getValorUnidad() : 0.0,
                            seguro != null ? seguro.getFactura() : "",
                            (seguro != null ? (seguro.getValorSeguro() != null ? seguro.getValorSeguro() : 0.0) : 0.0)
                    );


                })
                .collect(Collectors.toList());
    }
    @Override
    public Optional<Unidad> obtenerPorNoSerie(String noSerie) {
        return unidadRepository.findByNoSerie(noSerie);
    }
    @Override
    public List<Unidad> buscarPorUltimosDigitosSerie(String ultimos6) {
        return unidadRepository.findByNoSerieEndingWith(ultimos6);
    }
    @Transactional
    @Override
    public void eliminarPorNoSerie(String noSerie) {
        unidadRepository.deleteByNoSerie(noSerie);
    }



    @Override
    public Modelo buscarOCrearModeloPorNombre(String nombre) {
        return modeloRepository.findByNombre(nombre)
                .orElseGet(() -> {
                    Modelo nuevo = new Modelo();
                    nuevo.setNombre(nombre);
                    nuevo.setUso("CARGA"); // o "PASAJE", puedes ajustar según reglas
                    return modeloRepository.save(nuevo);
                });
    }
    @Override
    public Distribuidor buscarDistribuidorPorClave(String clave) {
        return distribuidorRepository.findByClaveDistribuidora(clave)
                .orElseThrow(() -> new RuntimeException("Distribuidor no encontrado con clave: " + clave));
    }

}
