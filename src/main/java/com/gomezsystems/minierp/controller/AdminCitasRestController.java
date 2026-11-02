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
    // y su monto se ingresa a Tesorería, Caja Fuerte y EBITDA.
    @PostMapping("/confirmar/{id}")
    @Transactional
    public ResponseEntity<String> confirmarCita(@PathVariable Long id) {
        Optional<Cita> citaOpt = citaRepository.findById(id);
        if (citaOpt.isPresent()) {
            Cita cita = citaOpt.get();
            cita.setEstado("CONFIRMADO");

            // 1. Determinar el monto real del cobro
            Double monto = 0.0;
            if (cita.getTotalPagado() != null && cita.getTotalPagado() > 0) {
                monto = cita.getTotalPagado().doubleValue();
            } else if (cita.getMontoPagado() != null && cita.getMontoPagado() > 0) {
                monto = cita.getMontoPagado();
            } else if (cita.getMontoTotal() != null && cita.getMontoTotal() > 0) {
                monto = cita.getMontoTotal();
            } else if (cita.getNombreTratamiento() != null && !cita.getNombreTratamiento().isEmpty()) {
                try {
                    List<Tratamiento> tratamientos = tratamientoRepository.findAll();
                    for (Tratamiento t : tratamientos) {
                        if (t.getNombre() != null && t.getNombre().equalsIgnoreCase(cita.getNombreTratamiento())) {
                            monto = (t.getPrecio() != null) ? t.getPrecio().doubleValue() : 0.0;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Asegurar que la cita conserve el valor numérico
            if ((cita.getTotalPagado() == null || cita.getTotalPagado() == 0) && monto > 0) {
                cita.setTotalPagado(monto.intValue());
                cita.setMontoTotal(monto);
                cita.setMontoPagado(monto);
            }
            citaRepository.save(cita);

            // 2. Vincular o crear la Venta para que ingrese a Tesorería, Caja y EBITDA
            boolean ventaActualizada = false;
            if (cita.getCliente() != null) {
                List<Venta> posiblesVentas = ventaRepository.findByClienteIdOrderByFechaHoraDesc(cita.getCliente().getId());
                for (Venta v : posiblesVentas) {
                    if ("RESERVADO".equalsIgnoreCase(v.getEstado()) || "PENDIENTE".equalsIgnoreCase(v.getEstado()) || "VERIFICANDO_PAGO".equalsIgnoreCase(v.getEstado())) {
                        v.setEstado("PAGADO");
                        v.setCierreAplicado(false); // Entra a la caja activa del turno actual
                        v.setFechaHora(LocalDateTime.now()); // Fecha de confirmación para Tesorería y EBITDA
                        
                        if (monto > 0 && (v.getTotalPagado() == null || v.getTotalPagado() <= 0)) {
                            v.setMontoTotal(monto);
                            v.setTotalPagado(monto);
                        }
                        if (cita.getSocia() != null && v.getSocia() == null) {
                            v.setSocia(cita.getSocia());
                        }
                        
                        // Cálculo de comisiones
                        if (v.getSocia() != null && (v.getMontoSocia() == null || v.getMontoSocia() == 0.0)) {
                            Double pct = (v.getSocia().getPorcentajeSocia() != null) ? v.getSocia().getPorcentajeSocia() : 0.0;
                            Double valBase = (v.getTotalPagado() != null) ? v.getTotalPagado() : monto;
                            Double mSocia = valBase * (pct / 100.0);
                            v.setMontoSocia(mSocia);
                            v.setMontoSpa(valBase - mSocia);
                        }
                        
                        ventaRepository.save(v);
                        ventaActualizada = true;
                        break; 
                    }
                }
            }

            // Si no existía una venta pendiente previa, crearla inmediatamente
            if (!ventaActualizada) {
                Venta nuevaVenta = new Venta();
                nuevaVenta.setCliente(cita.getCliente());
                nuevaVenta.setSocia(cita.getSocia());
                nuevaVenta.setDetalle((cita.getNombreTratamiento() != null && !cita.getNombreTratamiento().isEmpty()) 
                    ? cita.getNombreTratamiento() 
                    : "Servicio Spa");
                nuevaVenta.setMontoTotal(monto);
                nuevaVenta.setTotalPagado(monto);
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
                    Double mSocia = monto * (pct / 100.0);
                    nuevaVenta.setMontoSocia(mSocia);
                    nuevaVenta.setMontoSpa(monto - mSocia);
                } else {
                    nuevaVenta.setMontoSpa(monto);
                    nuevaVenta.setMontoSocia(0.0);
                }

                ventaRepository.save(nuevaVenta);
            }

            return ResponseEntity.ok("Cita confirmada e ingresada exitosamente a Tesorería, Caja y EBITDA");
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
