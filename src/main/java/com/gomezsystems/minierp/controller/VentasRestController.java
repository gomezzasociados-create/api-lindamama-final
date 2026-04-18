package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Cliente;
import com.gomezsystems.minierp.model.Venta;
import com.gomezsystems.minierp.model.CorteZ;
import com.gomezsystems.minierp.model.Egreso;
import com.gomezsystems.minierp.repository.ClienteRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import com.gomezsystems.minierp.repository.CorteZRepository;
import com.gomezsystems.minierp.model.Cita;
import com.gomezsystems.minierp.model.Socia;
import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.model.PaqueteComprado;
import com.gomezsystems.minierp.repository.PaqueteCompradoRepository;
import com.gomezsystems.minierp.repository.SociaRepository;
import com.gomezsystems.minierp.repository.EgresoRepository;
import com.gomezsystems.minierp.service.WhatsappService;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pos")
public class VentasRestController {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CorteZRepository corteZRepository;

    @Autowired
    private EgresoRepository egresoRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private SociaRepository sociaRepository;

    @Autowired
    private WhatsappService whatsappService;

    @Autowired
    private PaqueteCompradoRepository paqueteCompradoRepository;

    // ==========================================
    // 1. BUSCADOR DE CLIENTES PARA LA CAJA
    // ==========================================
    @GetMapping("/buscar-cliente")
    public List<Cliente> buscarCliente(@RequestParam("query") String query) {
        List<Cliente> todos = clienteRepository.findAll();
        List<Cliente> filtrados = new ArrayList<>();
        String q = query.toLowerCase();

        for(Cliente c : todos) {
            if((c.getNombre() != null && c.getNombre().toLowerCase().contains(q)) ||
                    (c.getTelefono() != null && c.getTelefono().contains(q))) {
                filtrados.add(c);
            }
        }
        return filtrados;
    }

    // ==========================================
    // 2. PROCESADOR DE VENTAS Y COBROS (FUSIONADO)
    // ==========================================
    @PostMapping("/venta-directa")
    @Transactional
    public String ventaDirecta(@RequestBody Map<String, Object> datos) {
        try {
            // --- A. MANEJO DEL CLIENTE ---
            Cliente clienteVenta = null;
            String clienteIdStr = String.valueOf(datos.get("clienteId"));

            if (clienteIdStr != null && !clienteIdStr.equals("null") && !clienteIdStr.isEmpty()) {
                Long cId = Long.parseLong(clienteIdStr);
                clienteVenta = clienteRepository.findById(cId).orElse(null);
            }

            if (clienteVenta == null) {
                clienteVenta = new Cliente();
                clienteVenta.setNombre((String) datos.get("nuevoClienteNombre"));
                clienteVenta.setTelefono((String) datos.get("nuevoClienteTelefono"));
                clienteVenta.setCorreo((String) datos.get("nuevoClienteCorreo"));
                clienteVenta = clienteRepository.save(clienteVenta);
            }

            // --- B. CREACIÓN DEL TICKET DE VENTA (Tesorería) ---
            Venta v = new Venta();
            v.setCliente(clienteVenta);

            Double total = Double.parseDouble(datos.get("totalPagado").toString());
            v.setMontoTotal(total);

            // Respetamos propina si la envían (Propina 100% para la socia)
            Double propinaIngresada = 0.0;
            if (datos.containsKey("propina") && datos.get("propina") != null) {
                propinaIngresada = Double.parseDouble(datos.get("propina").toString());
                v.setPropina(propinaIngresada);
            }

            String detalle = (String) datos.get("nombreTratamiento");
            if (detalle == null || detalle.isEmpty()) detalle = "Venta POS Spa";
            v.setDetalle(detalle);

            String metodo = (String) datos.get("metodoPago");
            v.setTipoPago(metodo);

            // Manejo de Socia y Comisiones Exigente (Matemática perfecta)
            Socia sociaGuardada = null;
            if (datos.containsKey("sociaId") && datos.get("sociaId") != null && !datos.get("sociaId").toString().isEmpty()) {
                Long sociaId = Long.parseLong(datos.get("sociaId").toString());
                sociaGuardada = sociaRepository.findById(sociaId).orElse(null);
                
                if (sociaGuardada != null) {
                    v.setSocia(sociaGuardada);
                    Double pctSocia = sociaGuardada.getPorcentajeSocia() != null ? sociaGuardada.getPorcentajeSocia() : 0.0;
                    
                    // La comisión se saca sobre el total real del servicio (montoTotal)
                    // La propina extra va íntegra a la socia, sin sufrir porcentaje del Spa
                    Double montoSocia = (double) Math.round(total * (pctSocia / 100.0)) + propinaIngresada; 
                    v.setMontoSocia(montoSocia);
                    
                    // El spa retiene el resto del pago del servicio
                    v.setMontoSpa((double) Math.round(total - (total * (pctSocia / 100.0))));
                } else {
                    v.setMontoSpa(total);
                }
            } else {
                v.setMontoSpa(total);
            }

            // Si es Fiado, actualizamos la deuda del cliente (sobre el servicio, no incluimos propina al fiado)
            String estadoVenta = "PAGADO";
            if ("FIADO".equals(metodo) || "CREDITO".equals(metodo)) {
                estadoVenta = "PENDIENTE";
                Double deudaAnterior = clienteVenta.getDeudaActiva() != null ? clienteVenta.getDeudaActiva() : 0.0;
                clienteVenta.setDeudaActiva(deudaAnterior + total);
                clienteRepository.save(clienteVenta);
            } else if ("MERCADO_PAGO".equals(metodo) || "MERCADOPAGO".equals(metodo)) {
                estadoVenta = "RESERVADO";
            }
            v.setEstado(estadoVenta);

            Venta ventaGuardada = ventaRepository.save(v);

            // --- B.2 CREACIÓN DE LA CITA ---
            Cita citaReservada = null;
            if (datos.containsKey("fechaHora") && datos.get("fechaHora") != null && !datos.get("fechaHora").toString().isEmpty()) {
                try {
                    Cita cita = new Cita();
                    cita.setCliente(clienteVenta);
                    cita.setNombreTratamiento(detalle);
                    cita.setSocia(sociaGuardada);
                    cita.setEstado("MERCADO_PAGO".equals(metodo) || "MERCADOPAGO".equals(metodo) ? "RESERVADO" : "CONFIRMADO");
                    cita.setTotalPagado(total.intValue());
                    cita.setMetodoPago(metodo);
                    cita.setFechaHora(java.time.LocalDateTime.parse(datos.get("fechaHora").toString()));
                    cita.setFechaCreacion(java.time.LocalDateTime.now()); // Para el bloqueo de 10 min
                    citaReservada = citaRepository.save(cita);
                } catch (Exception ex) {
                    System.err.println("Error creando la cita desde POS: " + ex.getMessage());
                }
            }

            // --- C. MANEJO DEL PAGO (MERCADO PAGO) ---
            if ("MERCADO_PAGO".equals(metodo) || "MERCADOPAGO".equals(metodo)) {
                MercadoPagoConfig.setAccessToken("APP_USR-5751871946510432-080417-64053e5e178846a2dff9e962057f1032-735099817");

                PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                        .title(ventaGuardada.getDetalle())
                        .quantity(1)
                        .unitPrice(new BigDecimal(total))
                        .currencyId("CLP")
                        .build();

                PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                        .success("https://lindamama.gomezz.space/api/pagos/exito")
                        .pending("https://lindamama.gomezz.space/pos")
                        .failure("https://lindamama.gomezz.space/pos")
                        .build();

                PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                        .items(Collections.singletonList(itemRequest))
                        .backUrls(backUrls)
                        .externalReference((citaReservada != null && citaReservada.getId() != null) ? "CITA_" + citaReservada.getId() + "_VENTA_" + ventaGuardada.getId() : "VENTA_" + ventaGuardada.getId())
                        .build();

                PreferenceClient client = new PreferenceClient();
                Preference preference = client.create(preferenceRequest);

                return preference.getInitPoint();
            }

            return "OK";

        } catch (Exception e) {
            System.err.println("Error en POS: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // ========================================================
    // 3. REGISTRAR EGRESO (Salidas de caja chica)
    // ========================================================
    @PostMapping("/egresos")
    public ResponseEntity<Egreso> registrarEgreso(@RequestBody Egreso egreso) {
        Egreso nuevoEgreso = egresoRepository.save(egreso);
        return ResponseEntity.ok(nuevoEgreso);
    }

    // ========================================================
    // 4. ESTADO DE LA CAJA FUERTE (Matemática Financiera Spa)
    // ========================================================
    @GetMapping("/caja-fuerte")
    public Map<String, Object> estadoCajaFuerte() {
        List<Venta> activas = ventaRepository.findByCierreAplicadoFalseOrderByFechaHoraDesc();
        List<Egreso> egresos = egresoRepository.findByCierreAplicadoFalseOrderByFechaHoraDesc();

        Double tEfectivo = 0.0, tFiado = 0.0, tTarjeta = 0.0, tTransf = 0.0, tPropinas = 0.0, tEgresos = 0.0;
        Double tGananciaSpa = 0.0, tDeudaSocias = 0.0;
        
        Map<String, Double> deudaPorSocia = new java.util.HashMap<>();

        for (Venta v : activas) {
            if ("RESERVADO".equals(v.getEstado())) continue;

            Double pagado = v.getTotalPagado() != null ? v.getTotalPagado() : (v.getMontoTotal() != null ? v.getMontoTotal() : 0.0);
            if (v.getPropina() != null) tPropinas += v.getPropina();

            String tipo = v.getTipoPago() != null ? v.getTipoPago().toUpperCase() : "EFECTIVO";

            if (tipo.contains("EFECTIVO") || tipo.contains("CONTADO")) tEfectivo += pagado;
            else if (tipo.contains("FIADO")) tFiado += pagado;
            else if (tipo.contains("TARJETA") || tipo.contains("MERCADOPAGO") || tipo.contains("MERCADO_PAGO")) tTarjeta += pagado;
            else if (tipo.contains("TRANSFERENCIA")) tTransf += pagado;

            // Comisiones
            tGananciaSpa += (v.getMontoSpa() != null ? v.getMontoSpa() : pagado);
            if (v.getMontoSocia() != null && v.getMontoSocia() > 0 && v.getSocia() != null) {
                tDeudaSocias += v.getMontoSocia();
                String nombreSocia = v.getSocia().getNombre();
                deudaPorSocia.put(nombreSocia, deudaPorSocia.getOrDefault(nombreSocia, 0.0) + v.getMontoSocia());
            }
        }

        for (Egreso e : egresos) {
            if ("EFECTIVO".equalsIgnoreCase(e.getMetodoSalida())) {
                tEgresos += e.getMonto() != null ? e.getMonto() : 0.0;
            }
        }

        Double cajonFisico = tEfectivo - tEgresos;

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("ventas", activas);
        response.put("egresos", egresos);
        response.put("totalEfectivo", tEfectivo);
        response.put("totalFiado", tFiado);
        response.put("totalTarjeta", tTarjeta);
        response.put("totalTransferencia", tTransf);
        response.put("totalPropinas", tPropinas);
        response.put("totalEgresos", tEgresos);
        response.put("efectivoEnCajon", Math.max(0, cajonFisico));
        response.put("gananciaSpa", tGananciaSpa);
        response.put("deudaSociasTotal", tDeudaSocias);
        response.put("deudaPorSocia", deudaPorSocia);
        response.put("numVentas", activas.size());

        return response;
    }

    // ========================================================
    // 5. CORTE Z (Cerrar Turno)
    // ========================================================
    @PostMapping("/ejecutar-corte")
    @Transactional
    public String ejecutarCorteCaja() {
        try {
            List<Venta> activas = ventaRepository.findByCierreAplicadoFalseOrderByFechaHoraDesc();
            List<Egreso> egresos = egresoRepository.findByCierreAplicadoFalseOrderByFechaHoraDesc();

            if(activas.isEmpty() && egresos.isEmpty()) return "SIN_MOVIMIENTOS";

            Double tEfe = 0.0, tFia = 0.0, tTar = 0.0, tTra = 0.0, tEgr = 0.0;

            for(Venta v : activas) {
                if ("RESERVADO".equals(v.getEstado())) continue;

                Double pagado = v.getTotalPagado() != null ? v.getTotalPagado() : v.getMontoTotal();
                String tipo = v.getTipoPago() != null ? v.getTipoPago().toUpperCase() : "";

                if(tipo.contains("EFECTIVO") || tipo.contains("CONTADO")) tEfe += pagado;
                else if(tipo.contains("FIADO")) tFia += pagado;
                else if(tipo.contains("TARJETA") || tipo.contains("MERCADOPAGO") || tipo.contains("MERCADO_PAGO")) tTar += pagado;
                else if(tipo.contains("TRANSFERENCIA")) tTra += pagado;

                v.setCierreAplicado(true);
            }

            for(Egreso e : egresos) {
                if ("EFECTIVO".equalsIgnoreCase(e.getMetodoSalida())) tEgr += e.getMonto();
                e.setCierreAplicado(true);
            }

            ventaRepository.saveAll(activas);
            egresoRepository.saveAll(egresos);

            CorteZ corte = new CorteZ();
            corte.setTotalEfectivo(tEfe);
            corte.setTotalFiado(tFia);
            corte.setTotalTarjeta(tTar);
            corte.setTotalTransferencia(tTra);
            corte.setTotalEgresos(tEgr);
            corte.setEfectivoEsperadoCajon(tEfe - tEgr);
            corte.setDetalleInforme("Cierre Spa. Gastos de caja: $" + tEgr + " | Efectivo Neto en Cajón: $" + (tEfe - tEgr));

            corteZRepository.save(corte);
            return "OK";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // ========================================================
    // 6. DEVOLUCIONES (Eliminar Ticket)
    // ========================================================
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminarVenta(@PathVariable Long id) {
        Optional<Venta> vOpt = ventaRepository.findById(id);
        if (vOpt.isPresent()) {
            Venta v = vOpt.get();
            
            // Blindaje Contable: No modificar facturación ya cerrada en un reporte Z
            if (v.isCierreAplicado()) {
                System.err.println("Intento de borrado de venta ya cerrada en Caja (ID: " + v.getId() + ")");
                return ResponseEntity.status(403).body("Este ticket ya pertenece a un Corte Z cerrado. Debe generar una reversión manual.");
            }

            if (("FIADO".equals(v.getTipoPago()) || "CREDITO".equals(v.getTipoPago())) && v.getCliente() != null) {
                Cliente c = v.getCliente();
                Double deudaActual = c.getDeudaActiva() != null ? c.getDeudaActiva() : 0.0;
                c.setDeudaActiva(Math.max(0, deudaActual - v.getMontoTotal()));
                clienteRepository.save(c);
            }
            ventaRepository.delete(v);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    // ========================================================
    // 7. HISTORIAL POR CLIENTE (KARDEX)
    // ========================================================
    @GetMapping("/ventas/cliente/{clienteId}")
    public List<Venta> getVentasPorCliente(@PathVariable Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaHoraDesc(clienteId);
    }

    // ========================================================
    // 8. PAQUETES (Venta y Listado)
    // ========================================================
    @GetMapping("/paquetes")
    public List<PaqueteComprado> obtenerPaquetesActivos() {
        return paqueteCompradoRepository.findAll();
    }

    @PostMapping("/paquetes/{clienteId}")
    @Transactional
    public ResponseEntity<?> venderPaquete(@PathVariable Long clienteId, @RequestBody PaqueteComprado datos) {
        Cliente c = clienteRepository.findById(clienteId).orElse(null);
        if (c == null) return ResponseEntity.badRequest().body("Cliente no encontrado");

        // 1. Guardar el paquete original
        datos.setCliente(c);
        datos.setSesionesRestantes(datos.getTotalSesiones());
        datos.setEstadoActivo(true);
        datos.setStatusPago("PAGADO");
        PaqueteComprado paqueteGuardado = paqueteCompradoRepository.save(datos);

        // 2. Registrar la venta inicial (Abono) en la Caja Spa para el Z-Cut
        Venta abono = new Venta();
        abono.setCliente(c);
        abono.setMontoTotal(datos.getMontoTotal());
        abono.setTotalPagado(datos.getMontoAbonado());
        abono.setDetalle("Venta Paquete: " + datos.getNombreTratamiento());
        abono.setTipoPago("EFECTIVO"); // Asumimos efectivo en vista, se podría ampliar
        abono.setEstado("PAGADO");
        abono.setMontoSpa(datos.getMontoAbonado()); 
        abono.setPaqueteOrigen(paqueteGuardado);
        abono.setFechaHora(java.time.LocalDateTime.now());
        
        ventaRepository.save(abono);

        // 3. Registrar Deuda en el cliente
        Double deudaRestante = datos.getMontoTotal() - datos.getMontoAbonado();
        if (deudaRestante > 0) {
            c.setDeudaActiva((c.getDeudaActiva() != null ? c.getDeudaActiva() : 0.0) + deudaRestante);
            clienteRepository.save(c);
        }

        return ResponseEntity.ok(paqueteGuardado);
    }
}
