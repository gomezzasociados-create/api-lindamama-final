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
import com.mercadopago.exceptions.MPApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/pagos")
public class PagoRestController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @PostMapping("/crear-preferencia")
    public String crearPreferencia(@RequestBody Map<String, Object> datos) {
        try {
            // 1. CONFIGURACIÓN TOKEN
            MercadoPagoConfig.setAccessToken("APP_USR-5751871946510432-080417-64053e5e178846a2dff9e962057f1032-735099817");

            // 2. EXTRACCIÓN DE DATOS
            String titulo = (String) datos.get("titulo");
            String precioStr = String.valueOf(datos.get("precio"));
            BigDecimal precio = new BigDecimal(precioStr);
            String fechaAgendamientoStr = (String) datos.get("fechaAgendamiento");

            // Datos de la clienta (Vienen del modal de la agenda)
            String nombreCli = (String) datos.get("nombre");
            String telefonoCli = (String) datos.get("telefono");

            // 3. REGISTRO DE CLIENTA EN CRM
            Cliente clienteVenta = new Cliente();
            clienteVenta.setNombre(nombreCli);
            clienteVenta.setTelefono(telefonoCli);
            // Guardamos la clienta primero para tener su ID
            clienteVenta = clienteRepository.save(clienteVenta);

            // 4. CREACIÓN DE LA CITA
            Cita nuevaCita = new Cita();
            nuevaCita.setCliente(clienteVenta);
            nuevaCita.setNombreTratamiento(titulo);
            nuevaCita.setTotalPagado(precio.intValue());
            nuevaCita.setEstado("RESERVADO");
            nuevaCita.setFechaCreacion(LocalDateTime.now());

            // 🔥 LIMPIEZA DE FECHA (Mata el error de "unparsed text")
            if (fechaAgendamientoStr != null && !fechaAgendamientoStr.isEmpty()) {
                // Tomamos solo los primeros 19 caracteres: YYYY-MM-DDTHH:mm:ss
                String fechaLimpia = fechaAgendamientoStr.substring(0, 19);
                nuevaCita.setFechaHora(LocalDateTime.parse(fechaLimpia));
            }

            // Guardamos la cita asociada a la clienta
            Cita citaGuardada = citaRepository.save(nuevaCita);

            // 5. MERCADO PAGO PREFERENCE
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title(titulo)
                    .quantity(1)
                    .unitPrice(precio)
                    .currencyId("CLP")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("http://localhost:8080/api/pagos/exito")
                    .pending("http://localhost:8080/catalogo")
                    .failure("http://localhost:8080/agenda")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .backUrls(backUrls)
                    .externalReference(String.valueOf(citaGuardada.getId()))
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            // Devolvemos el link para que el frontend redirija
            return preference.getInitPoint();

        } catch (MPApiException apiException) {
            System.err.println("🚨 Error MP: " + apiException.getApiResponse().getContent());
            return "Error MP: " + apiException.getApiResponse().getContent();
        } catch (Exception e) {
            System.err.println("Error interno: " + e.getMessage());
            return "Error al crear pago: " + e.getMessage();
        }
    }

    @GetMapping("/exito")
    public RedirectView pagoExitoso(
            @RequestParam(value = "collection_id", required = false) String collectionId,
            @RequestParam(value = "collection_status", required = false) String collectionStatus,
            @RequestParam(value = "external_reference", required = false) String externalReference) {

        if ("approved".equals(collectionStatus) && externalReference != null) {
            try {
                Long idCita = Long.parseLong(externalReference);
                Optional<Cita> citaOpt = citaRepository.findById(idCita);

                if (citaOpt.isPresent()) {
                    Cita cita = citaOpt.get();
                    cita.setEstado("CONFIRMADO"); // Pasa a verde/cyan
                    citaRepository.save(cita);
                }
            } catch (Exception e) {
                System.err.println("Error confirmando pago: " + e.getMessage());
            }
        }
        return new RedirectView("http://localhost:8080/catalogo?pago=exito");
    }
}