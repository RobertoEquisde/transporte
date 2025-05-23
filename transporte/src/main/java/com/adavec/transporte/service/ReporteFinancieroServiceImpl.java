package com.adavec.transporte.service;

import com.adavec.transporte.dto.ReporteFinancieroDTO;
import com.adavec.transporte.model.Unidad;
import com.adavec.transporte.model.Seguro;
import com.adavec.transporte.model.Cobros;
import com.adavec.transporte.repository.UnidadRepository;
import com.adavec.transporte.repository.SeguroRepository;
import com.adavec.transporte.repository.CobrosRepository;
import com.adavec.transporte.service.ReporteFinancieroService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class ReporteFinancieroServiceImpl implements ReporteFinancieroService {

    private final UnidadRepository unidadRepository;
    private final SeguroRepository seguroRepository;
    private final CobrosRepository cobrosRepository;

    public ReporteFinancieroServiceImpl(UnidadRepository unidadRepository, SeguroRepository seguroRepository, CobrosRepository cobrosRepository) {
        this.unidadRepository = unidadRepository;
        this.seguroRepository = seguroRepository;
        this.cobrosRepository = cobrosRepository;
    }

    @Override
    public List<ReporteFinancieroDTO> obtenerDatosFinancierosPorMes(YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fin = mes.atEndOfMonth();

        return unidadRepository.findByDebisFechaBetween(inicio, fin).stream().map(unidad -> {
            ReporteFinancieroDTO dto = new ReporteFinancieroDTO();

            // Modelo y distribuidor
            dto.setNoSerie(unidad.getNoSerie());
            dto.setModelo(unidad.getModelo() != null ? unidad.getModelo().getNombre() : "SIN MODELO");
            dto.setDistribuidora(unidad.getDistribuidor() != null ? unidad.getDistribuidor().getNombreDistribuidora() : "SIN DISTRIBUIDORA");
            dto.setClaveDistribuidor(unidad.getDistribuidor() != null ? unidad.getDistribuidor().getClaveDistribuidora() : "");

            // Valor unidad e intereses base
            Double valorUnidad = unidad.getValorUnidad() != null ? unidad.getValorUnidad() : 0.0;
            dto.setValorUnidad(valorUnidad);

            // Fechas
            String fechaDebis = unidad.getDebisFecha() != null ? unidad.getDebisFecha().toString() : "";
            dto.setFechaFactura(fechaDebis);
            dto.setFechaInteres(fechaDebis);  // mismo que debisFecha en este contexto
            dto.setFechaProceso(LocalDate.now().toString());

            // Seguro
            Seguro seguro = seguroRepository.findByUnidadId(unidad.getId());
            dto.setNumeroFactura(seguro != null ? seguro.getFactura() : "");
            dto.setCuotaSeguro(valorUnidad * 0.00324);
            dto.setSeguro(valorUnidad * 0.00134);

            // Cobros
            Cobros cobros = cobrosRepository.findByUnidadId(unidad.getId()).stream().findFirst().orElse(null);
            // Actualizar fechas desde Cobros si existe, si no usar fechaDebis como fallback
            if (cobros != null) {
                // Fecha de factura desde cobros
                dto.setFechaFactura(cobros.getFechaTraslado() != null ?
                        cobros.getFechaTraslado().toString() : fechaDebis);

                // Fecha de interés desde cobros
                dto.setFechaInteres(cobros.getFechaInteres() != null ?
                        cobros.getFechaInteres().toString() : fechaDebis);

                dto.setImporteTraslado(cobros.getTarifaUnica());
                dto.setFondoEstrella(cobros.getFondoEstrella());
                dto.setDias(cobros.getDias());
                dto.setCuotaAsociacion(cobros.getCuotaAsociacion());
            } else {
                // Si no hay cobros, usar valores por defecto
                dto.setFechaFactura(fechaDebis);
                dto.setFechaInteres(fechaDebis);
                dto.setImporteTraslado(0.0);
                dto.setFondoEstrella(0.0);
                dto.setDias(0);
                dto.setCuotaAsociacion(0.0);
            }

            return dto;
        }).collect(Collectors.toList());
    }
}
