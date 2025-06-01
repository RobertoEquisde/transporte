package com.adavec.transporte.service;

import com.adavec.transporte.dto.ActualizarCobroRequest;
import com.adavec.transporte.dto.CobroDTO;
import com.adavec.transporte.dto.CrearCobroRequest;
import com.adavec.transporte.model.Cobros;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.repository.CobrosRepository;
import com.adavec.transporte.repository.UnidadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CobrosService {

    private final CobrosRepository cobrosRepository;
    private final UnidadRepository unidadRepository;

    public CobrosService(CobrosRepository cobrosRepository, UnidadRepository unidadRepository) {
        this.cobrosRepository = cobrosRepository;
        this.unidadRepository = unidadRepository;
    }

    public Cobros registrarCobro(CrearCobroRequest request) {
        Unidad unidad = unidadRepository.findById(request.getUnidadId())
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada"));

        Cobros cobro = new Cobros();
        cobro.setUnidad(unidad);
        cobro.setTarifaUnica(request.getTarifaUnica());
        cobro.setCuotaAsociacion(request.getCuotaAsociacion());
        cobro.setFondoEstrella(request.getFondoEstrella());
        cobro.setDias(request.getDias());
        cobro.setFechaTraslado(LocalDate.parse(request.getFechaTraslado()));

        return cobrosRepository.save(cobro);
    }

    public List<CobroDTO> obtenerPorUnidad(Integer unidadId) {
        return cobrosRepository.findByUnidadId(unidadId).stream().map(c -> {
            CobroDTO dto = new CobroDTO();
            dto.setId(c.getId());
            dto.setUnidadId(c.getUnidad().getId());
            dto.setTarifaUnica(c.getTarifaUnica());
            dto.setCuotaAsociacion(c.getCuotaAsociacion());
            dto.setFondoEstrella(c.getFondoEstrella());
            dto.setDias(c.getDias());
            dto.setFechaTraslado(c.getFechaTraslado().toString());
            return dto;
        }).toList();
    }
    public Cobros guardar(Cobros cobro) {
        return cobrosRepository.save(cobro);
    }

    public Cobros actualizarCobro(Integer id, CrearCobroRequest request) {
        // 1. Buscar el cobro existente
        Cobros cobroExistente = cobrosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cobro no encontrado"));

        // 2. Validar y obtener la unidad
        Unidad unidad = unidadRepository.findById(request.getUnidadId())
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada"));

        // 3. Actualizar campos
        cobroExistente.setUnidad(unidad);
        cobroExistente.setTarifaUnica(request.getTarifaUnica());
        cobroExistente.setCuotaAsociacion(request.getCuotaAsociacion());
        cobroExistente.setFondoEstrella(request.getFondoEstrella());
        cobroExistente.setDias(request.getDias());
        cobroExistente.setFechaTraslado(LocalDate.parse(request.getFechaTraslado()));

        // 4. Guardar cambios
        return cobrosRepository.save(cobroExistente);
    }
    public Cobros actualizarCobroPorUnidad(Integer unidadId, ActualizarCobroRequest request) {
        // 1. Buscar cobros por ID de unidad
        List<Cobros> cobros = cobrosRepository.findByUnidadId(unidadId);

        // 2. Verificar que existan cobros para esa unidad
        if (cobros.isEmpty()) {
            throw new RuntimeException("No se encontró cobro para la unidad: " + unidadId);
        }

        // 3. Si hay múltiples cobros, tomar el más reciente
        Cobros cobroAActualizar = cobros.stream()
                .max((c1, c2) -> c1.getFechaTraslado().compareTo(c2.getFechaTraslado()))
                .orElseThrow(() -> new RuntimeException("Error al obtener el cobro"));

        // 4. Actualizar solo los campos necesarios
        cobroAActualizar.setTarifaUnica(request.getTarifaUnica());
        cobroAActualizar.setCuotaAsociacion(request.getCuotaAsociacion());
        cobroAActualizar.setFondoEstrella(request.getFondoEstrella());

        // 5. Guardar cambios
        return cobrosRepository.save(cobroAActualizar);
    }
    public void eliminarCobro(Integer id) {
        // Verificar existencia antes de eliminar
        if (!cobrosRepository.existsById(id)) {
            throw new RuntimeException("Cobro no encontrado");
        }
        cobrosRepository.deleteById(id);
    }

}
