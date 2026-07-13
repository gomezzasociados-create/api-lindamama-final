package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Socia;
import com.gomezsystems.minierp.model.Venta;
import com.gomezsystems.minierp.model.PagoComision;
import com.gomezsystems.minierp.repository.SociaRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import com.gomezsystems.minierp.repository.PagoComisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/socias")
public class SociaRestController {

    @Autowired
    private SociaRepository sociaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private PagoComisionRepository pagoComisionRepository;

    @GetMapping
    public List<Socia> listarTodas() {
        return sociaRepository.findAll();
    }

    @GetMapping("/activas")
    public List<Socia> listarActivas() {
        return sociaRepository.findByActivaTrue();
    }

    @PostMapping
    public ResponseEntity<Socia> crearOActualizarSocia(@RequestBody Socia socia) {
        // Validación para máximo 10 socias (si es nueva)
        if (socia.getId() == null) {
            long count = sociaRepository.count();
            if (count >= 10) {
                return ResponseEntity.badRequest().build(); // Límite alcanzado
            }
        }
        
        Socia guardada = sociaRepository.save(socia);
        return ResponseEntity.ok(guardada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarSocia(@PathVariable Long id) {
        Optional<Socia> socia = sociaRepository.findById(id);
        if (socia.isPresent()) {
            // En vez de borrar físicamente podríamos desactivar, pero si piden borrar la vacante:
            sociaRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- NUEVAS FUNCIONES DE LIQUIDACIÓN ---

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> obtenerBalance(@PathVariable Long id) {
        Optional<Socia> sociaOpt = sociaRepository.findById(id);
        if (sociaOpt.isEmpty()) return ResponseEntity.notFound().build();

        List<Venta> pendientes = ventaRepository.findBySociaIdAndLiquidadoSociaFalseOrderByFechaHoraDesc(id);
        List<PagoComision> historial = pagoComisionRepository.findBySociaIdOrderByFechaHoraDesc(id);
        Double totalPendiente = ventaRepository.sumMontoSociaBySociaIdAndLiquidadoSociaFalse(id);

        Map<String, Object> response = new HashMap<>();
        response.put("socia", sociaOpt.get());
        response.put("pendientes", pendientes);
        response.put("totalPendiente", totalPendiente != null ? totalPendiente : 0.0);
        response.put("historialPagos", historial);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/liquidar")
    @Transactional
    public ResponseEntity<?> liquidarComisiones(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Optional<Socia> sociaOpt = sociaRepository.findById(id);
        if (sociaOpt.isEmpty()) return ResponseEntity.notFound().build();

        List<Venta> pendientes = ventaRepository.findBySociaIdAndLiquidadoSociaFalseOrderByFechaHoraDesc(id);
        if (pendientes.isEmpty()) return ResponseEntity.badRequest().body("No hay comisiones pendientes para liquidar.");

        Double montoTotal = ventaRepository.sumMontoSociaBySociaIdAndLiquidadoSociaFalse(id);

        // 1. Crear el registro de pago
        PagoComision pago = new PagoComision();
        pago.setSocia(sociaOpt.get());
        pago.setMonto(montoTotal);
        pago.setMedioPago((String) datos.getOrDefault("medioPago", "EFECTIVO"));
        pago.setComentarios((String) datos.getOrDefault("comentarios", "Liquidación de comisiones acumuladas"));
        pagoComisionRepository.save(pago);

        // 2. Marcar ventas como liquidadas
        for (Venta v : pendientes) {
            v.setLiquidadoSocia(true);
        }
        ventaRepository.saveAll(pendientes);

        return ResponseEntity.ok(pago);
    }

    @PostMapping("/upload-foto")
    public ResponseEntity<?> subirFotoSocia(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo está vacío");
        }
        try {
            java.io.File uploadDir = new java.io.File("uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String extension = "";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String nuevoNombre = "socia_" + System.currentTimeMillis() + extension;

            java.nio.file.Path rutaDestino = java.nio.file.Paths.get("uploads", nuevoNombre);
            java.nio.file.Files.copy(file.getInputStream(), rutaDestino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Map<String, String> response = new HashMap<>();
            response.put("fotoUrl", "/uploads/" + nuevoNombre);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al guardar archivo: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/reporte-diario")
    public ResponseEntity<?> obtenerReporteDiario(@PathVariable Long id) {
        Optional<Socia> sociaOpt = sociaRepository.findById(id);
        if (sociaOpt.isEmpty()) return ResponseEntity.notFound().build();

        java.time.LocalDateTime inicioHoy = java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime finHoy = java.time.LocalDate.now().atTime(23, 59, 59);

        List<Venta> ventasHoy = ventaRepository.findBySociaIdAndFechaHoraBetweenOrderByFechaHoraDesc(id, inicioHoy, finHoy);
        
        Double totalGanadoSocia = 0.0;
        List<Map<String, Object>> desgloseServicios = new ArrayList<>();

        for (Venta v : ventasHoy) {
            if ("RESERVADO".equals(v.getEstado())) continue; // saltar reservas web no pagadas
            
            Double montoServicio = v.getTotalPagado() != null ? v.getTotalPagado() : v.getMontoTotal();
            Double comisionSocia = v.getMontoSocia() != null ? v.getMontoSocia() : 0.0;
            totalGanadoSocia += comisionSocia;

            Map<String, Object> item = new HashMap<>();
            item.put("detalle", v.getDetalle());
            item.put("monto", montoServicio);
            item.put("comision", comisionSocia);
            desgloseServicios.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("socia", sociaOpt.get().getNombre());
        response.put("telefono", sociaOpt.get().getTelefono());
        response.put("servicios", desgloseServicios);
        response.put("totalGanado", totalGanadoSocia);

        return ResponseEntity.ok(response);
    }
}
