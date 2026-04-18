package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Cita;
import com.gomezsystems.minierp.model.Venta;
import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/citas")
public class AdminCitasRestController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    // 🔥 1. CONFIRMAR (El chulito verde)
    @PostMapping("/confirmar/{id}")
    @Transactional
    public ResponseEntity<String> confirmarCita(@PathVariable Long id) {
        Optional<Cita> citaOpt = citaRepository.findById(id);
        if (citaOpt.isPresent()) {
            Cita cita = citaOpt.get();
            cita.setEstado("CONFIRMADO");
            citaRepository.save(cita);

            if (cita.getCliente() != null) {
                java.util.List<Venta> posiblesVentas = ventaRepository.findByClienteIdOrderByFechaHoraDesc(cita.getCliente().getId());
                for (Venta v : posiblesVentas) {
                    if ("RESERVADO".equals(v.getEstado()) || "PENDIENTE".equals(v.getEstado())) {
                        v.setEstado("PAGADO");
                        ventaRepository.save(v);
                        break; 
                    }
                }
            }

            return ResponseEntity.ok("Cita confirmada");
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
                // Si viene YYYY-MM-DDTHH:mm (16 caracteres), le ponemos los segundos :00
                if (fechaFinal.length() == 16) {
                    fechaFinal += ":00";
                }
                // Si viene con milisegundos o zona horaria (más de 19), lo cortamos limpio
                else if (fechaFinal.length() > 19) {
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