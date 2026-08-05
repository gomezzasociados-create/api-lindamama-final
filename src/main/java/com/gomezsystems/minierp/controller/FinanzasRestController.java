package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.*;
import com.gomezsystems.minierp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finanzas")
public class FinanzasRestController {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private EgresoRepository egresoRepository;
    @Autowired private PagoComisionRepository pagoComisionRepository;

    @GetMapping("/ebitda")
    public ResponseEntity<Map<String, Object>> getEbitdaReport(
            @RequestParam("inicio") String inicioStr,
            @RequestParam("fin") String finStr) {

        try {
            LocalDate inicio = LocalDate.parse(inicioStr);
            LocalDate fin = LocalDate.parse(finStr);

            LocalDateTime inicioLDT = inicio.atStartOfDay();
            LocalDateTime finLDT = fin.atTime(LocalTime.MAX);

            // Consultar datos en base de datos
            List<Venta> ventas = ventaRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicioLDT, finLDT);
            List<Egreso> egresos = egresoRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicioLDT, finLDT);
            List<PagoComision> pagosComisiones = pagoComisionRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicioLDT, finLDT);

            // Mapear ventas simplificadas
            List<Map<String, Object>> ventasMapped = ventas.stream().map(v -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", v.getId());
                map.put("fecha", v.getFechaHora().toString());
                map.put("clienteNombre", v.getCliente() != null ? v.getCliente().getNombre() : "Cliente General");
                map.put("sociaNombre", v.getSocia() != null ? v.getSocia().getNombre() : "Sin profesional");
                map.put("detalle", v.getDetalle());
                map.put("montoTotal", v.getMontoTotal() != null ? v.getMontoTotal() : 0.0);
                map.put("propina", v.getPropina() != null ? v.getPropina() : 0.0);
                map.put("totalPagado", v.getTotalPagado() != null ? v.getTotalPagado() : 0.0);
                map.put("montoSpa", v.getMontoSpa() != null ? v.getMontoSpa() : 0.0);
                map.put("montoSocia", v.getMontoSocia() != null ? v.getMontoSocia() : 0.0);
                map.put("tipoPago", v.getTipoPago());
                map.put("estado", v.getEstado());
                return map;
            }).collect(Collectors.toList());

            // Mapear egresos simplificados
            List<Map<String, Object>> egresosMapped = egresos.stream().map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", e.getId());
                map.put("fecha", e.getFechaHora().toString());
                map.put("descripcion", e.getDescripcion());
                map.put("monto", e.getMonto() != null ? e.getMonto() : 0.0);
                map.put("metodoSalida", e.getMetodoSalida());
                map.put("esInversion", e.getEsInversion());
                map.put("vidaUtilMeses", e.getVidaUtilMeses());
                map.put("categoria", e.getCategoria());
                map.put("valorResidual", e.getValorResidual());
                return map;
            }).collect(Collectors.toList());

            // Mapear pagos de comisiones
            List<Map<String, Object>> pagosMapped = pagosComisiones.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("fecha", p.getFechaHora().toString());
                map.put("sociaNombre", p.getSocia() != null ? p.getSocia().getNombre() : "Socia General");
                map.put("monto", p.getMonto() != null ? p.getMonto() : 0.0);
                map.put("medioPago", p.getMedioPago());
                map.put("comentarios", p.getComentarios());
                return map;
            }).collect(Collectors.toList());

            // Totales base
            double totalVentasGross = 0.0; // Ventas pagadas
            double totalVentasSpa = 0.0;
            double totalVentasSocia = 0.0;
            double totalPropinas = 0.0;
            double totalComisionesPagadas = pagosComisiones.stream().mapToDouble(p -> p.getMonto() != null ? p.getMonto() : 0.0).sum();

            for (Venta v : ventas) {
                if ("PAGADO".equalsIgnoreCase(v.getEstado())) {
                    totalVentasGross += (v.getMontoTotal() != null ? v.getMontoTotal() : 0.0);
                    totalVentasSpa += (v.getMontoSpa() != null ? v.getMontoSpa() : 0.0);
                    totalVentasSocia += (v.getMontoSocia() != null ? v.getMontoSocia() : 0.0);
                    totalPropinas += (v.getPropina() != null ? v.getPropina() : 0.0);
                }
            }

            // Desglose de egresos del periodo (OPEX y CAPEX)
            double totalOpex = 0.0;
            double totalCapex = 0.0;
            Map<String, Double> opexPorCategoria = new HashMap<>();
            Map<String, Double> capexPorCategoria = new HashMap<>();

            for (Egreso e : egresos) {
                double monto = e.getMonto() != null ? e.getMonto() : 0.0;
                String cat = e.getCategoria() != null ? e.getCategoria() : "OTROS";
                if (e.getEsInversion()) {
                    totalCapex += monto;
                    capexPorCategoria.put(cat, capexPorCategoria.getOrDefault(cat, 0.0) + monto);
                } else {
                    totalOpex += monto;
                    opexPorCategoria.put(cat, opexPorCategoria.getOrDefault(cat, 0.0) + monto);
                }
            }

            // Cálculo de depreciación de inversiones (CAPEX) activas en el periodo
            double totalDepreciacion = 0.0;
            List<Map<String, Object>> detallesInversiones = new ArrayList<>();
            List<Egreso> todasInversiones = egresoRepository.findByEsInversionTrue();

            for (Egreso inv : todasInversiones) {
                if (inv.getVidaUtilMeses() == null || inv.getVidaUtilMeses() <= 0) continue;

                LocalDate fechaAdq = inv.getFechaHora().toLocalDate();
                double monto = inv.getMonto() != null ? inv.getMonto() : 0.0;
                double valorResidual = inv.getValorResidual() != null ? inv.getValorResidual() : 0.0;
                double valorDepreciable = monto - valorResidual;
                if (valorDepreciable <= 0) continue;

                int vidaUtilMeses = inv.getVidaUtilMeses();
                LocalDate fechaFinVidaUtil = fechaAdq.plusMonths(vidaUtilMeses);

                // Si la vida útil del activo terminó antes del inicio del periodo consultado, 
                // o si el activo se adquirió después de la fecha de fin del periodo consultado, no aplica depreciación.
                if (fechaFinVidaUtil.isBefore(inicio) || fechaAdq.isAfter(fin)) {
                    continue;
                }

                // Intersección de fechas
                LocalDate startDep = fechaAdq.isAfter(inicio) ? fechaAdq : inicio;
                LocalDate endDep = fechaFinVidaUtil.isBefore(fin) ? fechaFinVidaUtil : fin;

                if (!startDep.isAfter(endDep)) {
                    // Días de depreciación en este rango
                    long diasRango = java.time.temporal.ChronoUnit.DAYS.between(startDep, endDep) + 1;

                    // Días totales de vida útil
                    long diasTotales = java.time.temporal.ChronoUnit.DAYS.between(fechaAdq, fechaFinVidaUtil);
                    if (diasTotales <= 0) diasTotales = vidaUtilMeses * 30L;

                    // Depreciación del periodo
                    double depPeriodo = valorDepreciable * ((double) diasRango / diasTotales);
                    if (depPeriodo < 0) depPeriodo = 0.0;
                    if (depPeriodo > valorDepreciable) depPeriodo = valorDepreciable;

                    totalDepreciacion += depPeriodo;

                    // Desgaste acumulado hasta el final del periodo consultado (o fin de vida útil si ocurrió antes)
                    LocalDate finCalculoAcumulado = fechaFinVidaUtil.isBefore(fin) ? fechaFinVidaUtil : fin;
                    long diasTranscurridos = java.time.temporal.ChronoUnit.DAYS.between(fechaAdq, finCalculoAcumulado);
                    if (diasTranscurridos < 0) diasTranscurridos = 0;
                    double depAcumulada = valorDepreciable * ((double) diasTranscurridos / diasTotales);
                    if (depAcumulada > valorDepreciable) depAcumulada = valorDepreciable;

                    double pctDesgaste = (depAcumulada / valorDepreciable) * 100.0;

                    Map<String, Object> det = new HashMap<>();
                    det.put("id", inv.getId());
                    det.put("descripcion", inv.getDescripcion());
                    det.put("fechaAdquisicion", inv.getFechaHora().toString());
                    det.put("monto", monto);
                    det.put("valorResidual", valorResidual);
                    det.put("vidaUtilMeses", vidaUtilMeses);
                    det.put("depreciacionPeriodo", depPeriodo);
                    det.put("depreciacionAcumulada", depAcumulada);
                    det.put("porcentajeDesgaste", pctDesgaste);
                    det.put("categoria", inv.getCategoria());
                    
                    double mesesTranscurridos = (double) diasTranscurridos / 30.4375;
                    det.put("vidaUtilRestanteMeses", Math.max(0.0, Math.round((vidaUtilMeses - mesesTranscurridos) * 10.0) / 10.0));
                    detallesInversiones.add(det);
                }
            }

            // EBITDA Devengado: Ingresos Spa - Gastos Operativos (OPEX)
            double ebitdaDevengado = totalVentasSpa - totalOpex;

            // EBITDA de Caja: Total Real Recibido (Gross + Propinas) - Egresos Totales (OPEX + CAPEX) - Comisiones Reales Pagadas
            double ebitdaCaja = (totalVentasGross + totalPropinas) - totalOpex - totalCapex - totalComisionesPagadas;

            // EBIT Devengado (Utilidad Operativa Real): EBITDA - Depreciación
            double ebitDevengado = ebitdaDevengado - totalDepreciacion;

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalVentasGross", totalVentasGross);
            summary.put("totalVentasSpa", totalVentasSpa);
            summary.put("totalVentasSocia", totalVentasSocia);
            summary.put("totalPropinas", totalPropinas);
            summary.put("totalEgresos", totalOpex + totalCapex);
            summary.put("totalOpex", totalOpex);
            summary.put("totalCapex", totalCapex);
            summary.put("totalComisionesPagadas", totalComisionesPagadas);
            summary.put("totalDepreciacion", totalDepreciacion);
            summary.put("ebitdaDevengado", ebitdaDevengado);
            summary.put("ebitdaCaja", ebitdaCaja);
            summary.put("ebitDevengado", ebitDevengado);

            Map<String, Object> response = new HashMap<>();
            response.put("ventas", ventasMapped);
            response.put("egresos", egresosMapped);
            response.put("pagosComisiones", pagosMapped);
            response.put("inversionesDepreciacion", detallesInversiones);
            response.put("opexPorCategoria", opexPorCategoria);
            response.put("capexPorCategoria", capexPorCategoria);
            response.put("summary", summary);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }
}
