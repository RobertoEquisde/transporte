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
            dto.setCuotaSeguro(valorUnidad * 0.0324);     // 3.24%
            dto.setSeguro(valorUnidad * 0.0134);          // 1.34%

            // Cobros
            Cobros cobros = cobrosRepository.findByUnidadId(unidad.getId()).stream().findFirst().orElse(null);
            dto.setImporteTraslado(cobros != null ? cobros.getTarifaUnica() : 0.0);
            dto.setFondoEstrella(cobros != null ? cobros.getFondoEstrella() : 0.0);
            dto.setDias(cobros != null ? cobros.getDias() : 0);
            dto.setCuotaAsociacion(cobros != null ? cobros.getCuotaAsociacion() : 0.0);

            return dto;
        }).collect(Collectors.toList());
    }
}
