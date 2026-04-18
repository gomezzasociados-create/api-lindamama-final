package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Cita;
import com.gomezsystems.minierp.model.Cliente;
import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.ClienteRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import com.gomezsystems.minierp.model.Socia;
import com.gomezsystems.minierp.model.Venta;
import com.gomezsystems.minierp.repository.SociaRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import com.gomezsystems.minierp.service.WhatsappService;
import com.mercadopago.exceptions.MPApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/pagos")
public class PagoRestController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private SociaRepository sociaRepository;

    @Autowired
    private WhatsappService whatsappService;

    @PostMapping("/crear-preferencia")
    @Transactional
    public String crearPreferencia(@RequestBody Map<String, Object> datos) {
        try {
            // 1. CONFIGURACIÓN TOKEN (Tu token actual)
            MercadoPagoConfig.setAccessToken("APP_USR-5751871946510432-080417-64053e5e178846a2dff9e962057f1032-735099817");

            // 2. EXTRACCIÓN DE DATOS CON DEFENSA
            String tituloCompleto = (String) datos.getOrDefault("titulo", "Servicio Spa");
            String titulo = tituloCompleto;
            Long sociaId = null;
            if (tituloCompleto != null && tituloCompleto.contains("|")) {
                String[] parts = tituloCompleto.split("\\|");
                titulo = parts[0];
                try {
                    sociaId = Long.parseLong(parts[1]);
                } catch (Exception e) { /* ignorar parsing de socia */ }
            }

            // Validación de Precios
            Object precioObj = datos.get("precio");
            if (precioObj == null) throw new RuntimeException("El precio del servicio es nulo.");
            BigDecimal precio = new BigDecimal(String.valueOf(precioObj));

            Object montoTotalObj = datos.get("montoTotal");
            BigDecimal montoTotalServicio = (montoTotalObj != null) ? new BigDecimal(String.valueOf(montoTotalObj)) : precio;

            String fechaAgendamientoStr = (String) datos.get("fechaAgendamiento");
            String metodoPago = (String) datos.getOrDefault("metodoPago", "mercadopago");

            // Datos del cliente
            String nombreCli = (String) datos.getOrDefault("nombre", "Cliente Web");
            String telefonoCli = (String) datos.getOrDefault("telefono", "");
            String emailCli = (String) datos.get("email");

            // 3. REGISTRO DE CLIENTA EN CRM
            Cliente clienteVenta = new Cliente();
            clienteVenta.setNombre(nombreCli);
            clienteVenta.setTelefono(telefonoCli);
            if (emailCli != null && !emailCli.isEmpty()) {
                clienteVenta.setCorreo(emailCli);
            }
            clienteVenta = clienteRepository.save(clienteVenta);

            Socia sociaAsignada = null;
            if (sociaId != null) {
                sociaAsignada = sociaRepository.findById(sociaId).orElse(null);
            }

            // 4. CREACIÓN DE LA CITA
            Cita nuevaCita = new Cita();
            nuevaCita.setCliente(clienteVenta);
            nuevaCita.setNombreTratamiento(titulo);
            nuevaCita.setTotalPagado(precio.intValue());
            nuevaCita.setMontoPagado(precio.doubleValue());
            nuevaCita.setMontoTotal(montoTotalServicio.doubleValue());
            nuevaCita.setEstado("RESERVADO"); 
            nuevaCita.setFechaCreacion(LocalDateTime.now());
            nuevaCita.setMetodoPago(metodoPago);
            nuevaCita.setSocia(sociaAsignada);

            if (fechaAgendamientoStr != null && fechaAgendamientoStr.length() >= 19) {
                try {
                    String fechaLimpia = fechaAgendamientoStr.substring(0, 19);
                    nuevaCita.setFechaHora(LocalDateTime.parse(fechaLimpia));
                } catch (Exception e) {
                    System.err.println("Error parseando fecha: " + fechaAgendamientoStr);
                }
            }

            Cita citaGuardada = citaRepository.save(nuevaCita);

            // 🔥 4.2. NOTIFICACIÓN WHATSAPP (CON ESCUDO PARA NO CANCELAR LA VENTA)
            try {
                if (sociaAsignada != null && sociaAsignada.getTelefono() != null && !sociaAsignada.getTelefono().isEmpty()) {
                    String fechaLegible = nuevaCita.getFechaHora() != null ? nuevaCita.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "Por confirmar";
                    String msg = "🌸 *NUEVO AGENDAMIENTO* 🌸\n\n" +
                                 "Hola *" + sociaAsignada.getNombre() + "*,\n" +
                                 "Tienes una nueva cita asignada:\n" +
                                 "- *Cliente:* " + nombreCli + "\n" +
                                 "- *Servicio:* " + titulo + "\n" +
                                 "- *Fecha:* " + fechaLegible + "\n" +
                                 "- *Contacto Cliente:* " + telefonoCli + "\n\n" +
                                 "_*Linda Mamá Spa*_ ✨";
                    whatsappService.enviarMensajeTexto(sociaAsignada.getTelefono(), msg);
                }
            } catch (Exception e) {
                System.err.println("Aviso: No se pudo enviar WhatsApp a la socia, pero la venta continúa: " + e.getMessage());
            }

            // 4.5. CREACIÓN DE LA VENTA (KARDEX)
            Venta nuevaVenta = new Venta();
            nuevaVenta.setDetalle(titulo + " (Web)");
            nuevaVenta.setMontoTotal(precio.doubleValue());
            nuevaVenta.setTotalPagado(precio.doubleValue());
            nuevaVenta.setFechaHora(LocalDateTime.now());
            String tipoPago = "transferencia".equals(metodoPago) ? "TRANSFERENCIA" : "MERCADOPAGO";
            nuevaVenta.setTipoPago(tipoPago);
            nuevaVenta.setEstado("RESERVADO");
            
            if (sociaAsignada != null) {
                nuevaVenta.setSocia(sociaAsignada);
                Double pctSocia = (sociaAsignada.getPorcentajeSocia() != null) ? sociaAsignada.getPorcentajeSocia() : 0.0;
                Double montoSocia = precio.doubleValue() * (pctSocia / 100.0);
                nuevaVenta.setMontoSocia(montoSocia);
                nuevaVenta.setMontoSpa(precio.doubleValue() - montoSocia);
            } else {
                nuevaVenta.setMontoSpa(precio.doubleValue());
            }

            nuevaVenta.setCliente(clienteVenta);
            ventaRepository.save(nuevaVenta);

            // 🔥 5. DEVOLUCIÓN SEGÚN MÉTODO 🔥
            if ("transferencia".equals(metodoPago)) {
                return "TRANSFERENCIA_OK|" + citaGuardada.getId();
            }

            // SI ES MERCADO PAGO, CREAMOS LA PREFERENCIA
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                .title(titulo)
                .quantity(1)
                .unitPrice(precio)
                .currencyId("CLP")
                .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(itemRequest);

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
                .backUrls(PreferenceBackUrlsRequest.builder()
                    .success("https://lindamama.gomerzz.space/catalogo?pago=exito")
                    .failure("https://lindamama.gomerzz.space/catalogo?pago=error")
                    .pending("https://lindamama.gomerzz.space/catalogo?pago=pendiente")
                    .build())
                .autoReturn("approved")
                .notificationUrl("https://bot.gomezz.space/webhook/pago-confirmado")
                .externalReference("CITA_" + citaGuardada.getId() + "_VENTA_" + nuevaVenta.getId())
                .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            return preference.getInitPoint();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error al procesar el pago: " + e.getMessage();
        }
    }

    @GetMapping("/exito")
    @Transactional
    public RedirectView pagoExitoso(
            @RequestParam(value = "collection_id", required = false) String collectionId,
            @RequestParam(value = "collection_status", required = false) String collectionStatus,
            @RequestParam(value = "external_reference", required = false) String externalReference) {

        if ("approved".equals(collectionStatus) && externalReference != null) {
            try {
                if (externalReference.contains("_VENTA_")) {
                    String[] parts = externalReference.split("_VENTA_");
                    if (parts[0].startsWith("CITA_")) {
                        Long idCita = Long.parseLong(parts[0].replace("CITA_", ""));
                        citaRepository.findById(idCita).ifPresent(c -> { c.setEstado("CONFIRMADO"); citaRepository.save(c); });
                    }
                    Long idVenta = Long.parseLong(parts[1]);
                    ventaRepository.findById(idVenta).ifPresent(v -> { v.setEstado("PAGADO"); ventaRepository.save(v); });
                } else if (externalReference.startsWith("VENTA_")) {
                    Long idVenta = Long.parseLong(externalReference.replace("VENTA_", ""));
                    ventaRepository.findById(idVenta).ifPresent(v -> { v.setEstado("PAGADO"); ventaRepository.save(v); });
                } else {
                    Long rawId = Long.parseLong(externalReference);
                    citaRepository.findById(rawId).ifPresent(c -> { c.setEstado("CONFIRMADO"); citaRepository.save(c); });
                }
            } catch (Exception e) {
                System.err.println("Error confirmando pago MP: " + e.getMessage());
            }
        }
        return new RedirectView("/catalogo?pago=exito");
    }
}