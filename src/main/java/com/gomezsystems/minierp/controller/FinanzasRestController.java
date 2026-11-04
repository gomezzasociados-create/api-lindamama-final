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
    @Autowired private SociaRepository sociaRepository;
    @Autowired private CorteZRepository corteZRepository;
    @Autowired private ConfiguracionSeguridadRepository configuracionSeguridadRepo;

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
            List<CorteZ> cortes = corteZRepository.findByFechaGeneracionBetweenOrderByFechaGeneracionDesc(inicioLDT, finLDT);

            // Mapear ventas simplificadas
            List<Map<String, Object>> ventasMapped = ventas.stream().map(v -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", v.getId());
                map.put("fecha", v.getFechaHora() != null ? v.getFechaHora().toString() : LocalDateTime.now().toString());
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
                map.put("cierreAplicado", v.isCierreAplicado());
                return map;
            }).collect(Collectors.toList());

            // Mapear egresos simplificados
            List<Map<String, Object>> egresosMapped = egresos.stream().map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", e.getId());
                map.put("fecha", e.getFechaHora() != null ? e.getFechaHora().toString() : LocalDateTime.now().toString());
                map.put("descripcion", e.getDescripcion());
                map.put("monto", e.getMonto() != null ? e.getMonto() : 0.0);
                map.put("metodoSalida", e.getMetodoSalida());
                map.put("esInversion", e.getEsInversion());
                map.put("vidaUtilMeses", e.getVidaUtilMeses());
                map.put("categoria", e.getCategoria());
                map.put("valorResidual", e.getValorResidual());
                map.put("cierreAplicado", e.isCierreAplicado());
                return map;
            }).collect(Collectors.toList());

            // Mapear pagos de comisiones
            List<Map<String, Object>> pagosMapped = pagosComisiones.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("fecha", p.getFechaHora() != null ? p.getFechaHora().toString() : LocalDateTime.now().toString());
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
                boolean esVentaValida = false;
                String est = v.getEstado() != null ? v.getEstado().toUpperCase() : "";
                if (v.isCierreAplicado()) {
                    esVentaValida = true; // Si está consolidada en un cierre de caja, cuenta sí o sí
                } else if ("PAGADO".equals(est) || "CONFIRMADO".equals(est) || "COMPLETADO".equals(est) || "FINALIZADO".equals(est)) {
                    esVentaValida = true;
                } else if (!"RESERVADO".equals(est) && !"PENDIENTE".equals(est)) {
                    esVentaValida = true;
                } else if (v.getTotalPagado() != null && v.getTotalPagado() > 0) {
                    esVentaValida = true;
                }

                if (esVentaValida) {
                    Double montoReal = (v.getTotalPagado() != null && v.getTotalPagado() > 0) ? v.getTotalPagado() : (v.getMontoTotal() != null ? v.getMontoTotal() : 0.0);
                    totalVentasGross += (v.getMontoTotal() != null ? v.getMontoTotal() : montoReal);
                    totalVentasSpa += (v.getMontoSpa() != null ? v.getMontoSpa() : montoReal);
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
                if (inv == null || inv.getFechaHora() == null) continue;
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

            // Mapear Cortes Z (Cierres de Caja)
            List<Map<String, Object>> cortesMapped = cortes.stream().map(c -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", c.getId());
                map.put("fecha", c.getFechaGeneracion() != null ? c.getFechaGeneracion().toString() : LocalDateTime.now().toString());
                map.put("totalEfectivo", c.getTotalEfectivo() != null ? c.getTotalEfectivo() : 0.0);
                map.put("totalTarjeta", c.getTotalTarjeta() != null ? c.getTotalTarjeta() : 0.0);
                map.put("totalTransferencia", c.getTotalTransferencia() != null ? c.getTotalTransferencia() : 0.0);
                map.put("totalFiado", c.getTotalFiado() != null ? c.getTotalFiado() : 0.0);
                map.put("totalEgresos", c.getTotalEgresos() != null ? c.getTotalEgresos() : 0.0);
                map.put("efectivoEsperadoCajon", c.getEfectivoEsperadoCajon() != null ? c.getEfectivoEsperadoCajon() : 0.0);
                map.put("detalleInforme", c.getDetalleInforme());
                double totalVentasCorte = (c.getTotalEfectivo() != null ? c.getTotalEfectivo() : 0.0)
                        + (c.getTotalTarjeta() != null ? c.getTotalTarjeta() : 0.0)
                        + (c.getTotalTransferencia() != null ? c.getTotalTransferencia() : 0.0)
                        + (c.getTotalFiado() != null ? c.getTotalFiado() : 0.0);
                map.put("totalVentas", totalVentasCorte);
                return map;
            }).collect(Collectors.toList());

            double totalCortesVentas = cortes.stream().mapToDouble(c -> 
                (c.getTotalEfectivo() != null ? c.getTotalEfectivo() : 0.0) +
                (c.getTotalTarjeta() != null ? c.getTotalTarjeta() : 0.0) +
                (c.getTotalTransferencia() != null ? c.getTotalTransferencia() : 0.0) +
                (c.getTotalFiado() != null ? c.getTotalFiado() : 0.0)
            ).sum();
            double totalCortesEfectivo = cortes.stream().mapToDouble(c -> c.getTotalEfectivo() != null ? c.getTotalEfectivo() : 0.0).sum();
            double totalCortesNeto = cortes.stream().mapToDouble(c -> c.getEfectivoEsperadoCajon() != null ? c.getEfectivoEsperadoCajon() : 0.0).sum();
            double totalCortesEgresos = cortes.stream().mapToDouble(c -> c.getTotalEgresos() != null ? c.getTotalEgresos() : 0.0).sum();

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

            summary.put("numCortesZ", cortes.size());
            summary.put("totalCortesVentas", totalCortesVentas);
            summary.put("totalCortesEfectivo", totalCortesEfectivo);
            summary.put("totalCortesNeto", totalCortesNeto);
            summary.put("totalCortesEgresos", totalCortesEgresos);

            Map<String, Object> response = new HashMap<>();
            response.put("ventas", ventasMapped);
            response.put("egresos", egresosMapped);
            response.put("pagosComisiones", pagosMapped);
            response.put("inversionesDepreciacion", detallesInversiones);
            response.put("cortes", cortesMapped);
            response.put("opexPorCategoria", opexPorCategoria);
            response.put("capexPorCategoria", capexPorCategoria);
            response.put("summary", summary);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("ventas", new ArrayList<>());
            fallback.put("egresos", new ArrayList<>());
            fallback.put("pagosComisiones", new ArrayList<>());
            fallback.put("inversionesDepreciacion", new ArrayList<>());
            fallback.put("cortes", new ArrayList<>());
            fallback.put("opexPorCategoria", new HashMap<>());
            fallback.put("capexPorCategoria", new HashMap<>());
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalVentasGross", 0.0);
            summary.put("totalVentasSpa", 0.0);
            summary.put("totalVentasSocia", 0.0);
            summary.put("totalPropinas", 0.0);
            summary.put("totalEgresos", 0.0);
            summary.put("totalOpex", 0.0);
            summary.put("totalCapex", 0.0);
            summary.put("totalComisionesPagadas", 0.0);
            summary.put("totalDepreciacion", 0.0);
            summary.put("ebitdaDevengado", 0.0);
            summary.put("ebitdaCaja", 0.0);
            summary.put("ebitDevengado", 0.0);
            summary.put("numCortesZ", 0);
            summary.put("totalCortesVentas", 0.0);
            summary.put("totalCortesEfectivo", 0.0);
            summary.put("totalCortesNeto", 0.0);
            summary.put("totalCortesEgresos", 0.0);
            fallback.put("summary", summary);
            fallback.put("error", e.getMessage());
            return ResponseEntity.ok(fallback);
        }
    }

    // ========================================================
    // EDICIÓN Y ELIMINACIÓN DE REGISTROS DE EBITDA Y CAJA
    // ========================================================

    // 1. VENTA: EDITAR
    @PutMapping("/ventas/{id}")
    public ResponseEntity<?> editarVenta(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Optional<Venta> vOpt = ventaRepository.findById(id);
        if (vOpt.isEmpty()) return ResponseEntity.notFound().build();

        Venta v = vOpt.get();

        if (datos.containsKey("detalle") && datos.get("detalle") != null) {
            v.setDetalle((String) datos.get("detalle"));
        }
        if (datos.containsKey("montoTotal") && datos.get("montoTotal") != null) {
            Double mTotal = Double.parseDouble(datos.get("montoTotal").toString());
            v.setMontoTotal(mTotal);
        }
        if (datos.containsKey("propina") && datos.get("propina") != null) {
            Double prop = Double.parseDouble(datos.get("propina").toString());
            v.setPropina(prop);
        }
        if (datos.containsKey("tipoPago") && datos.get("tipoPago") != null) {
            v.setTipoPago((String) datos.get("tipoPago"));
        }
        if (datos.containsKey("estado") && datos.get("estado") != null) {
            v.setEstado((String) datos.get("estado"));
        }

        if (datos.containsKey("sociaId")) {
            Object sIdObj = datos.get("sociaId");
            if (sIdObj != null && !sIdObj.toString().isEmpty() && !"null".equals(sIdObj.toString())) {
                Long sociaId = Long.parseLong(sIdObj.toString());
                Socia socia = sociaRepository.findById(sociaId).orElse(null);
                v.setSocia(socia);
            } else {
                v.setSocia(null);
            }
        }

        // Recalcular montos Spa / Socia
        Double total = v.getMontoTotal() != null ? v.getMontoTotal() : 0.0;
        Double propina = v.getPropina() != null ? v.getPropina() : 0.0;
        Socia socia = v.getSocia();
        if (socia != null) {
            Double pctSocia = socia.getPorcentajeSocia() != null ? socia.getPorcentajeSocia() : 0.0;
            Double montoSocia = (double) Math.round(total * (pctSocia / 100.0)) + propina;
            v.setMontoSocia(montoSocia);
            v.setMontoSpa((double) Math.round(total - (total * (pctSocia / 100.0))));
        } else {
            v.setMontoSpa(total);
            v.setMontoSocia(propina);
        }

        Venta guardada = ventaRepository.save(v);
        return ResponseEntity.ok(guardada);
    }

    // 1. VENTA: ELIMINAR
    @DeleteMapping("/ventas/{id}")
    public ResponseEntity<?> borrarVenta(@PathVariable Long id) {
        Optional<Venta> vOpt = ventaRepository.findById(id);
        if (vOpt.isPresent()) {
            ventaRepository.delete(vOpt.get());
            return ResponseEntity.ok().body(Map.of("mensaje", "Venta eliminada correctamente"));
        }
        return ResponseEntity.notFound().build();
    }

    // 2. EGRESO: EDITAR
    @PutMapping("/egresos/{id}")
    public ResponseEntity<?> editarEgreso(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Optional<Egreso> eOpt = egresoRepository.findById(id);
        if (eOpt.isEmpty()) return ResponseEntity.notFound().build();

        Egreso e = eOpt.get();

        if (datos.containsKey("descripcion") && datos.get("descripcion") != null) {
            e.setDescripcion((String) datos.get("descripcion"));
        }
        if (datos.containsKey("monto") && datos.get("monto") != null) {
            e.setMonto(Double.parseDouble(datos.get("monto").toString()));
        }
        if (datos.containsKey("metodoSalida") && datos.get("metodoSalida") != null) {
            e.setMetodoSalida((String) datos.get("metodoSalida"));
        }
        if (datos.containsKey("esInversion") && datos.get("esInversion") != null) {
            e.setEsInversion(Boolean.parseBoolean(datos.get("esInversion").toString()));
        }
        if (datos.containsKey("vidaUtilMeses") && datos.get("vidaUtilMeses") != null) {
            e.setVidaUtilMeses(Integer.parseInt(datos.get("vidaUtilMeses").toString()));
        }
        if (datos.containsKey("categoria") && datos.get("categoria") != null) {
            e.setCategoria((String) datos.get("categoria"));
        }
        if (datos.containsKey("valorResidual") && datos.get("valorResidual") != null) {
            e.setValorResidual(Double.parseDouble(datos.get("valorResidual").toString()));
        }

        Egreso guardado = egresoRepository.save(e);
        return ResponseEntity.ok(guardado);
    }

    // 2. EGRESO: ELIMINAR
    @DeleteMapping("/egresos/{id}")
    public ResponseEntity<?> borrarEgreso(@PathVariable Long id) {
        Optional<Egreso> eOpt = egresoRepository.findById(id);
        if (eOpt.isPresent()) {
            egresoRepository.delete(eOpt.get());
            return ResponseEntity.ok().body(Map.of("mensaje", "Egreso eliminado correctamente"));
        }
        return ResponseEntity.notFound().build();
    }

    // 3. PAGO COMISION: EDITAR
    @PutMapping("/pagos-comisiones/{id}")
    public ResponseEntity<?> editarPagoComision(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Optional<PagoComision> pOpt = pagoComisionRepository.findById(id);
        if (pOpt.isEmpty()) return ResponseEntity.notFound().build();

        PagoComision p = pOpt.get();

        if (datos.containsKey("monto") && datos.get("monto") != null) {
            p.setMonto(Double.parseDouble(datos.get("monto").toString()));
        }
        if (datos.containsKey("medioPago") && datos.get("medioPago") != null) {
            p.setMedioPago((String) datos.get("medioPago"));
        }
        if (datos.containsKey("comentarios") && datos.get("comentarios") != null) {
            p.setComentarios((String) datos.get("comentarios"));
        }
        if (datos.containsKey("sociaId")) {
            Object sIdObj = datos.get("sociaId");
            if (sIdObj != null && !sIdObj.toString().isEmpty() && !"null".equals(sIdObj.toString())) {
                Long sociaId = Long.parseLong(sIdObj.toString());
                Socia socia = sociaRepository.findById(sociaId).orElse(null);
                p.setSocia(socia);
            }
        }

        PagoComision guardado = pagoComisionRepository.save(p);
        return ResponseEntity.ok(guardado);
    }

    // 3. PAGO COMISION: ELIMINAR
    @DeleteMapping("/pagos-comisiones/{id}")
    public ResponseEntity<?> borrarPagoComision(@PathVariable Long id) {
        Optional<PagoComision> pOpt = pagoComisionRepository.findById(id);
        if (pOpt.isPresent()) {
            pagoComisionRepository.delete(pOpt.get());
            return ResponseEntity.ok().body(Map.of("mensaje", "Pago de comisión eliminado correctamente"));
        }
        return ResponseEntity.notFound().build();
    }

    // ========================================================
    // 4. MÉTODOS SEGUROS CON AUTORIZACIÓN PIN MAESTRO 6 DÍGITOS
    // ========================================================

    private boolean validarPinMaestro(String pin) {
        if (pin == null || pin.trim().isEmpty()) return false;
        ConfiguracionSeguridad config = configuracionSeguridadRepo.findByClave("PIN_MAESTRO_TESORERIA").orElse(null);
        String valido = config != null ? config.getValor() : "777777";
        return pin.trim().equals(valido);
    }

    // ELIMINACIÓN SEGURA DE PAGO / VENTA
    @PostMapping("/eliminar-pago-seguro/{id}")
    public ResponseEntity<?> eliminarPagoSeguro(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String pin = payload.get("pinSeguridad");
        if (!validarPinMaestro(pin)) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "mensaje", "Clave de seguridad de 6 dígitos incorrecta. Operación no autorizada."
            ));
        }

        Optional<Venta> vOpt = ventaRepository.findById(id);
        if (vOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", "error", "mensaje", "El pago o venta no fue encontrado."));
        }

        ventaRepository.delete(vOpt.get());
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "mensaje", "Pago / Venta eliminada correctamente de Tesorería y Finanzas."
        ));
    }

    // ELIMINACIÓN SEGURA DE GASTO / EGRESO
    @PostMapping("/eliminar-egreso-seguro/{id}")
    public ResponseEntity<?> eliminarEgresoSeguro(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String pin = payload.get("pinSeguridad");
        if (!validarPinMaestro(pin)) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "mensaje", "Clave de seguridad de 6 dígitos incorrecta. Operación no autorizada."
            ));
        }

        Optional<Egreso> eOpt = egresoRepository.findById(id);
        if (eOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", "error", "mensaje", "El egreso o gasto no fue encontrado."));
        }

        egresoRepository.delete(eOpt.get());
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "mensaje", "Gasto / Egreso eliminado correctamente de Tesorería y Finanzas."
        ));
    }

    // ELIMINACIÓN SEGURA DE CIERRE Z
    @PostMapping("/eliminar-corte-seguro/{id}")
    public ResponseEntity<?> eliminarCorteSeguro(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String pin = payload.get("pinSeguridad");
        if (!validarPinMaestro(pin)) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "mensaje", "Clave de seguridad de 6 dígitos incorrecta. Operación no autorizada."
            ));
        }

        Optional<CorteZ> czOpt = corteZRepository.findById(id);
        if (czOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", "error", "mensaje", "El Cierre Z no fue encontrado."));
        }

        corteZRepository.delete(czOpt.get());
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "mensaje", "Reporte de Cierre Z eliminado correctamente."
        ));
    }

    // REINICIO SEGURO DE FINANZAS
    @PostMapping("/reiniciar-seguro")
    public ResponseEntity<?> reiniciarFinanzasSeguro(@RequestBody Map<String, String> payload) {
        String pin = payload.get("pinSeguridad");
        String modo = payload.getOrDefault("modo", "TURNO_ACTUAL");

        if (!validarPinMaestro(pin)) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "mensaje", "Clave de seguridad de 6 dígitos incorrecta. Operación no autorizada."
            ));
        }

        if ("TURNO_ACTUAL".equalsIgnoreCase(modo)) {
            // Reiniciar únicamente movimientos abiertos del turno actual
            List<Venta> ventasTurno = ventaRepository.findByCierreAplicadoFalseOrderByFechaHoraDesc();
            if (!ventasTurno.isEmpty()) {
                ventaRepository.deleteAll(ventasTurno);
            }

            List<Egreso> egresosTurno = egresoRepository.findByCierreAplicadoFalseOrderByFechaHoraDesc();
            if (!egresosTurno.isEmpty()) {
                egresoRepository.deleteAll(egresosTurno);
            }

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "mensaje", "Caja del turno actual reiniciada exitosamente. Se eliminaron los movimientos no consolidados."
            ));
        } else if ("TODO".equalsIgnoreCase(modo)) {
            // Reinicio completo de finanzas
            egresoRepository.deleteAll();
            pagoComisionRepository.deleteAll();
            corteZRepository.deleteAll();
            ventaRepository.deleteAll();

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "mensaje", "Historial financiero reiniciado completamente a cero con éxito."
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "mensaje", "Modo de reinicio inválido."));
        }
    }
}

