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
            dto.setNoSerie(unidad.getNoSerie());
            dto.setModelo(unidad.getModelo() != null ? unidad.getModelo().getNombre() : "SIN MODELO");
            dto.setUso(unidad.getModelo() != null ? unidad.getModelo().getUso() : "N/A");
            dto.setDistribuidora(unidad.getDistribuidor() != null ? unidad.getDistribuidor().getNombreDistribuidora() : "SIN DISTRIBUIDORA");

            dto.setDistribuidora(unidad.getDistribuidor().getNombreDistribuidora());
            dto.setValorUnidad(unidad.getValorUnidad());

            Seguro seguro = seguroRepository.findByUnidadId(unidad.getId());
            dto.setValorSeguro(seguro != null ? seguro.getValorSeguro() : 0.0);
            dto.setCuotaSeguro(seguro != null ? seguro.getSeguroDistribuidor() : 0.0);
            dto.setFechaFactura(unidad.getDebisFecha() != null ? unidad.getDebisFecha().toString() : "");

            List<Cobros> cobrosList = cobrosRepository.findByUnidadId(unidad.getId());
            Cobros cobros = cobrosList.isEmpty() ? null : cobrosList.get(0);

            dto.setTarifaUnica(cobros != null ? cobros.getTarifaUnica() : 0.0);
            dto.setFondoEstrella(cobros != null ? cobros.getFondoEstrella() : 0.0);

            return dto;
        }).collect(Collectors.toList());
    }
}
