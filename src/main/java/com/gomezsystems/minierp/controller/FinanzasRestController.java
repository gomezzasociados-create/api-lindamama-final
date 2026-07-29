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

            // Totales para EBITDA Devengado (Accrued) y de Caja (Cash Flow)
            double totalVentasGross = 0.0; // Solo ventas pagadas
            double totalVentasSpa = 0.0;
            double totalVentasSocia = 0.0;
            double totalPropinas = 0.0;
            double totalEgresos = egresos.stream().mapToDouble(e -> e.getMonto() != null ? e.getMonto() : 0.0).sum();
            double totalComisionesPagadas = pagosComisiones.stream().mapToDouble(p -> p.getMonto() != null ? p.getMonto() : 0.0).sum();

            for (Venta v : ventas) {
                if ("PAGADO".equalsIgnoreCase(v.getEstado())) {
                    totalVentasGross += (v.getMontoTotal() != null ? v.getMontoTotal() : 0.0);
                    totalVentasSpa += (v.getMontoSpa() != null ? v.getMontoSpa() : 0.0);
                    totalVentasSocia += (v.getMontoSocia() != null ? v.getMontoSocia() : 0.0);
                    totalPropinas += (v.getPropina() != null ? v.getPropina() : 0.0);
                }
            }

            // EBITDA Devengado: Ingresos Brutos (montoTotal) - Comisiones Devengadas (montoSocia) - Egresos
            // Equivalente a: Ventas Netas del Spa (montoSpa) - Egresos
            double ebitdaDevengado = totalVentasSpa - totalEgresos;

            // EBITDA de Caja: Total Real Recibido (Gross + Propinas) - Egresos - Comisiones Reales Pagadas
            double ebitdaCaja = (totalVentasGross + totalPropinas) - totalEgresos - totalComisionesPagadas;

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalVentasGross", totalVentasGross);
            summary.put("totalVentasSpa", totalVentasSpa);
            summary.put("totalVentasSocia", totalVentasSocia);
            summary.put("totalPropinas", totalPropinas);
            summary.put("totalEgresos", totalEgresos);
            summary.put("totalComisionesPagadas", totalComisionesPagadas);
            summary.put("ebitdaDevengado", ebitdaDevengado);
            summary.put("ebitdaCaja", ebitdaCaja);

            Map<String, Object> response = new HashMap<>();
            response.put("ventas", ventasMapped);
            response.put("egresos", egresosMapped);
            response.put("pagosComisiones", pagosMapped);
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
