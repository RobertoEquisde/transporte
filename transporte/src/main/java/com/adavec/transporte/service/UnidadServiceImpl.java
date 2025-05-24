package com.adavec.transporte.service.impl;

import com.adavec.transporte.dto.UnidadDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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
        return distribuidorRepository.findFirstByClaveDistribuidora(clave)
                .orElseThrow(() -> new RuntimeException("Distribuidor no encontrado con clave: " + clave));
    }
    @Transactional
    @Override
    public Unidad actualizarUnidad(Integer id, UnidadDTO requestDTO) {
        Unidad unidadExistente = unidadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró unidad con ID: " + id));

        // Optional: Validate if the ID in DTO (if present) matches the path variable ID
        if (requestDTO.getId() != null && !requestDTO.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID en el cuerpo ("+ requestDTO.getId() +") no coincide con el ID en la URL ("+ id +").");
        }

        // Update direct fields if provided in the requestDTO
        // For PUT, nulls in request often mean "set to null". If only non-null fields should update, that's more like PATCH.
        // The checks below implement a "update if key is present and value is not null" for most fields,
        // and "update to null if key is present and value is empty string/specific indicator for dates"

        if (requestDTO.getNoSerie() != null) {
            unidadExistente.setNoSerie(requestDTO.getNoSerie());
        }
        if (requestDTO.getComentario() != null) {
            unidadExistente.setComentario(requestDTO.getComentario());
        }
        if (requestDTO.getOrigen() != null) {
            unidadExistente.setOrigen(requestDTO.getOrigen());
        }
        if (requestDTO.getReportadoA() != null) {
            unidadExistente.setReportadoA(requestDTO.getReportadoA());
        }
        if (requestDTO.getValorUnidad() != null) {
            unidadExistente.setValorUnidad(requestDTO.getValorUnidad());
        }

        // Handle date strings from DTO
        if (requestDTO.getDebisFecha() != null) { // If key "debisFecha" is present
            if (requestDTO.getDebisFecha().trim().isEmpty()) {
                unidadExistente.setDebisFecha(null); // Set to null if an empty string is passed
            } else {
                try {
                    unidadExistente.setDebisFecha(LocalDate.parse(requestDTO.getDebisFecha()));
                } catch (DateTimeParseException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de DebisFecha inválido. Use yyyy-MM-dd.", e);
                }
            }
        }

        if (requestDTO.getPagoDistribuidora() != null) { // If key "pagoDistribuidora" is present
            if (requestDTO.getPagoDistribuidora().trim().isEmpty()) {
                unidadExistente.setPagoDistribuidora(null); // Set to null if an empty string is passed
            } else {
                try {
                    unidadExistente.setPagoDistribuidora(LocalDate.parse(requestDTO.getPagoDistribuidora()));
                } catch (DateTimeParseException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de PagoDistribuidora inválido. Use yyyy-MM-dd.", e);
                }
            }
        }

        // Update Modelo if modelo DTO is present and contains an ID
        if (requestDTO.getModelo() != null && requestDTO.getModelo().getId() != null) {
            Modelo modelo = modeloRepository.findById(requestDTO.getModelo().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modelo no encontrado con ID: " + requestDTO.getModelo().getId()));
            unidadExistente.setModelo(modelo);
        } else if (requestDTO.getModelo() != null && requestDTO.getModelo().getId() == null) {
            // If you want to allow unsetting the modelo by passing `modelo: {}` or `modelo: {id: null}`
            // you could add: unidadExistente.setModelo(null);
            // For now, this case is ignored if ID is missing.
            // Or throw bad request: throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de Modelo es requerido para actualizar la asociación de modelo.");
        }


        // Update Distribuidor if distribuidor DTO is present and contains an ID
        if (requestDTO.getDistribuidor() != null && requestDTO.getDistribuidor().getId() != null) {
            Distribuidor distribuidor = distribuidorRepository.findById(requestDTO.getDistribuidor().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Distribuidor no encontrado con ID: " + requestDTO.getDistribuidor().getId()));
            unidadExistente.setDistribuidor(distribuidor);
        } else if (requestDTO.getDistribuidor() != null && requestDTO.getDistribuidor().getId() == null) {
            // Similar to Modelo, define behavior for unsetting or require ID.
            // e.g., unidadExistente.setDistribuidor(null);
            // Or throw bad request: throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de Distribuidor es requerido para actualizar la asociación de distribuidor.");
        }
        // Update Seguro if seguro DTO is present and contains an ID
        if (requestDTO.getSeguro() != null && requestDTO.getSeguro().getId() != null) {
            Seguro seguro = seguroRepository.findById(requestDTO.getSeguro().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Seguro no encontrado con ID: " + requestDTO.getSeguro().getId()));

            // Actualizar la relación desde el lado de Seguro
            seguro.setUnidad(unidadExistente);
            seguroRepository.save(seguro);
        } else if (requestDTO.getSeguro() != null && requestDTO.getSeguro().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de Seguro es requerido para actualizar la asociación de seguro.");
        }

        return unidadRepository.save(unidadExistente);
    }

}
