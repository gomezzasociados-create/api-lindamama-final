package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Cita;
import com.gomezsystems.minierp.model.Venta;
import com.gomezsystems.minierp.model.Tratamiento;
import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import com.gomezsystems.minierp.repository.TratamientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/citas")
public class AdminCitasRestController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private TratamientoRepository tratamientoRepository;

    // 🔥 1. CONFIRMAR / APROBAR (El chulito verde)
    // Al confirmar, la cita pasa a CONFIRMADO (desapareciendo de la lista de pendientes)
    // Solo si registró un pago real (abono o total), se ingresa a Tesorería, Caja Fuerte y EBITDA.
    // Si no registra pago ($0 o null), no genera ingresos ficticios.
    @PostMapping("/confirmar/{id}")
    @Transactional
    public ResponseEntity<String> confirmarCita(@PathVariable Long id) {
        Optional<Cita> citaOpt = citaRepository.findById(id);
        if (citaOpt.isPresent()) {
            Cita cita = citaOpt.get();
            cita.setEstado("CONFIRMADO");

            // 1. Determinar si existe un monto de pago real recibido (abono o pago total)
            Double pagoReal = 0.0;
            if (cita.getTotalPagado() != null && cita.getTotalPagado() > 0) {
                pagoReal = cita.getTotalPagado().doubleValue();
            } else if (cita.getMontoPagado() != null && cita.getMontoPagado() > 0) {
                pagoReal = cita.getMontoPagado();
            }

            // Guardar el estado confirmado de la cita
            citaRepository.save(cita);

            // 2. Si NO hubo pago registrado (pagoReal <= 0 o null):
            // No ingresa a Tesorería, Caja ni EBITDA para evitar ingresos fantasmas.
            if (pagoReal <= 0) {
                return ResponseEntity.ok("Cita confirmada con éxito. No se registró ingreso en Tesorería por no presentar pago previo.");
            }

            // 3. Si SÍ hubo pago real registrado:
            // Asegurar montos consistentes en la cita
            cita.setTotalPagado(pagoReal.intValue());
            cita.setMontoPagado(pagoReal);
            citaRepository.save(cita);

            Double montoTotalServicio = (cita.getMontoTotal() != null && cita.getMontoTotal() > 0) 
                ? cita.getMontoTotal() 
                : pagoReal;

            // Vincular o crear la Venta para que ingrese a Tesorería, Caja y EBITDA
            boolean ventaActualizada = false;
            if (cita.getCliente() != null) {
                List<Venta> posiblesVentas = ventaRepository.findByClienteIdOrderByFechaHoraDesc(cita.getCliente().getId());
                for (Venta v : posiblesVentas) {
                    if ("RESERVADO".equalsIgnoreCase(v.getEstado()) || "PENDIENTE".equalsIgnoreCase(v.getEstado()) || "VERIFICANDO_PAGO".equalsIgnoreCase(v.getEstado())) {
                        v.setEstado("PAGADO");
                        v.setCierreAplicado(false); // Entra a la caja activa del turno actual
                        v.setFechaHora(LocalDateTime.now()); // Fecha de confirmación para Tesorería y EBITDA
                        v.setMontoTotal(montoTotalServicio);
                        v.setTotalPagado(pagoReal);

                        if (cita.getSocia() != null) {
                            v.setSocia(cita.getSocia());
                        }

                        // Cálculo de comisiones sobre el dinero real recibido
                        if (v.getSocia() != null) {
                            Double pct = (v.getSocia().getPorcentajeSocia() != null) ? v.getSocia().getPorcentajeSocia() : 0.0;
                            Double mSocia = pagoReal * (pct / 100.0);
                            v.setMontoSocia(mSocia);
                            v.setMontoSpa(pagoReal - mSocia);
                        } else {
                            v.setMontoSpa(pagoReal);
                            v.setMontoSocia(0.0);
                        }

                        ventaRepository.save(v);
                        ventaActualizada = true;
                        break; 
                    }
                }
            }

            // Si no existía una venta previa, crearla con el pago real recibido
            if (!ventaActualizada) {
                Venta nuevaVenta = new Venta();
                nuevaVenta.setCliente(cita.getCliente());
                nuevaVenta.setSocia(cita.getSocia());
                nuevaVenta.setDetalle((cita.getNombreTratamiento() != null && !cita.getNombreTratamiento().isEmpty()) 
                    ? cita.getNombreTratamiento() 
                    : "Servicio Spa");
                nuevaVenta.setMontoTotal(montoTotalServicio);
                nuevaVenta.setTotalPagado(pagoReal);
                nuevaVenta.setPropina(0.0);

                String tipo = (cita.getMetodoPago() != null && !cita.getMetodoPago().isEmpty()) 
                    ? cita.getMetodoPago().toUpperCase() 
                    : "TRANSFERENCIA";
                nuevaVenta.setTipoPago(tipo);
                nuevaVenta.setEstado("PAGADO");
                nuevaVenta.setCierreAplicado(false); // Activa en la caja del turno
                nuevaVenta.setLiquidadoSocia(false);
                nuevaVenta.setFechaHora(LocalDateTime.now());

                if (cita.getSocia() != null) {
                    Double pct = (cita.getSocia().getPorcentajeSocia() != null) ? cita.getSocia().getPorcentajeSocia() : 0.0;
                    Double mSocia = pagoReal * (pct / 100.0);
                    nuevaVenta.setMontoSocia(mSocia);
                    nuevaVenta.setMontoSpa(pagoReal - mSocia);
                } else {
                    nuevaVenta.setMontoSpa(pagoReal);
                    nuevaVenta.setMontoSocia(0.0);
                }

                ventaRepository.save(nuevaVenta);
            }

            return ResponseEntity.ok("Cita confirmada e ingreso de $" + Math.round(pagoReal) + " registrado en Tesorería, Caja y EBITDA");
        }
        return ResponseEntity.badRequest().body("Cita no encontrada");
    }

    // 🔥 2. CANCELAR (La X roja)
    @PostMapping("/cancelar/{id}")
    @Transactional
    public ResponseEntity<String> cancelarCita(@PathVariable Long id) {
        if (citaRepository.existsById(id)) {
            citaRepository.deleteById(id);
            return ResponseEntity.ok("Cita eliminada");
        }
        return ResponseEntity.badRequest().body("Cita no encontrada");
    }

    // 🔥 2.1. ELIMINACIÓN MASIVA DE CITAS PENDIENTES
    @PostMapping("/eliminar-masivo")
    @Transactional
    public ResponseEntity<String> eliminarCitasMasivo(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("No se proporcionaron citas para eliminar");
        }
        try {
            int eliminadas = 0;
            for (Long id : ids) {
                if (id != null && citaRepository.existsById(id)) {
                    citaRepository.deleteById(id);
                    eliminadas++;
                }
            }
            return ResponseEntity.ok("Se eliminaron " + eliminadas + " citas seleccionadas exitosamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al eliminar citas: " + e.getMessage());
        }
    }

    // 🔥 3. REAGENDAR (El botón azul de reloj) - CORREGIDO Y BLINDADO
    @PostMapping("/reagendar/{id}")
    @Transactional
    public ResponseEntity<String> reagendarCita(@PathVariable Long id, @RequestParam("nuevaFecha") String nuevaFecha) {
        Optional<Cita> citaOpt = citaRepository.findById(id);

        if (citaOpt.isPresent()) {
            try {
                Cita cita = citaOpt.get();
                String fechaFinal = nuevaFecha;

                // 🛡️ Lógica de seguridad para el formato de fecha:
                if (fechaFinal.length() == 16) {
                    fechaFinal += ":00";
                } else if (fechaFinal.length() > 19) {
                    fechaFinal = fechaFinal.substring(0, 19);
                }

                cita.setFechaHora(LocalDateTime.parse(fechaFinal));
                citaRepository.save(cita);

                return ResponseEntity.ok("Cita reagendada con éxito");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Error al procesar la fecha: " + e.getMessage());
            }
        }
        return ResponseEntity.badRequest().body("Cita no encontrada");
    }
}
