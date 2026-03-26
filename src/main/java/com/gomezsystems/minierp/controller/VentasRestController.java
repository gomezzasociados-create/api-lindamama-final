package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Cita;
import com.gomezsystems.minierp.model.Cliente;
import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.ClienteRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pos") // 🔥 Cambiamos la ruta para que coincida con tu frontend
public class VentasRestController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    // ==========================================
    // 1. BUSCADOR DE CLIENTES PARA LA CAJA
    // ==========================================
    @GetMapping("/buscar-cliente")
    public List<Cliente> buscarCliente(@RequestParam("query") String query) {
        List<Cliente> todos = clienteRepository.findAll();
        List<Cliente> filtrados = new ArrayList<>();
        String q = query.toLowerCase();

        // Filtramos en memoria para no tener que modificar tu ClienteRepository
        for(Cliente c : todos) {
            if((c.getNombre() != null && c.getNombre().toLowerCase().contains(q)) ||
                    (c.getTelefono() != null && c.getTelefono().contains(q))) {
                filtrados.add(c);
            }
        }
        return filtrados;
    }

    // ==========================================
    // 2. PROCESADOR DE VENTAS Y COBROS
    // ==========================================
    @PostMapping("/venta-directa")
    public String ventaDirecta(@RequestBody Map<String, Object> datos) {
        try {
            // --- A. MANEJO DEL CLIENTE ---
            Cliente clienteVenta = null;
            String clienteIdStr = String.valueOf(datos.get("clienteId"));

            // Si viene un ID del buscador, lo usamos
            if (clienteIdStr != null && !clienteIdStr.equals("null") && !clienteIdStr.isEmpty()) {
                Long cId = Long.parseLong(clienteIdStr);
                clienteVenta = clienteRepository.findById(cId).orElse(null);
            }

            // Si no hay ID (clienta nueva que escribiste a mano), la creamos y guardamos en el CRM
            if (clienteVenta == null) {
                clienteVenta = new Cliente();
                clienteVenta.setNombre((String) datos.get("nuevoClienteNombre"));
                clienteVenta.setTelefono((String) datos.get("nuevoClienteTelefono"));
                clienteVenta.setCorreo((String) datos.get("nuevoClienteCorreo"));
                clienteVenta = clienteRepository.save(clienteVenta);
            }

            // --- B. MANEJO DE LA CITA / VENTA ---
            Cita nuevaCita = new Cita();
            nuevaCita.setCliente(clienteVenta);
            nuevaCita.setNombreTratamiento((String) datos.get("nombreTratamiento"));

            int total = Integer.parseInt(String.valueOf(datos.get("totalPagado")));
            nuevaCita.setTotalPagado(total);

            String metodo = (String) datos.get("metodoPago");
            nuevaCita.setMetodoPago(metodo);
            nuevaCita.setEstado((String) datos.get("estado")); // El HTML ya manda RESERVADO o CONFIRMADO

            // Ponemos el cronómetro por si es Mercado Pago
            nuevaCita.setFechaCreacion(LocalDateTime.now());

            String fechaStr = (String) datos.get("fechaHora");
            if (fechaStr != null && !fechaStr.isEmpty()) {
                nuevaCita.setFechaHora(LocalDateTime.parse(fechaStr));
            }

            // Guardamos la venta en la base de datos
            Cita citaGuardada = citaRepository.save(nuevaCita);

            // --- C. MANEJO DEL PAGO (LA MAGIA DE MP) ---
            if ("MERCADO_PAGO".equals(metodo)) {
                // 🔥 PON TU ACCESS TOKEN AQUÍ
                MercadoPagoConfig.setAccessToken("APP_USR-5751871946510432-080417-64053e5e178846a2dff9e962057f1032-735099817");

                PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                        .title(nuevaCita.getNombreTratamiento())
                        .quantity(1)
                        .unitPrice(new BigDecimal(total))
                        .currencyId("CLP")
                        .build();

                PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                        .success("http://localhost:8080/api/pagos/exito") // Reutilizamos el endpoint que ya tienes!
                        .pending("http://localhost:8080/pos")
                        .failure("http://localhost:8080/pos")
                        .build();

                PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                        .items(Collections.singletonList(itemRequest))
                        .backUrls(backUrls)
                        .externalReference(String.valueOf(citaGuardada.getId()))
                        .build();

                PreferenceClient client = new PreferenceClient();
                Preference preference = client.create(preferenceRequest);

                // Retornamos el link de pago para que la caja te redirija
                return preference.getInitPoint();
            }

            // Si es Efectivo o Transferencia, simplemente decimos que todo salió bien
            return "OK";

        } catch (Exception e) {
            System.err.println("Error en POS: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}