package com.gomezsystems.minierp.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsappService {

    // ========================================================
    // ⚠️ PEGA AQUÍ TUS DATOS REALES DE EVOLUTION API:
    // ========================================================
    private final String apiUrl = "";      // Ej: "https://api.gomezz.space" (Sin barra / al final)
    private final String instancia = "";   // Ej: "lindamama"
    private final String token = "";       // Tu Global Apikey o el de la instancia
    // ========================================================

    private final RestTemplate restTemplate = new RestTemplate();

    public void enviarMensajeTexto(String numeroDestino, String mensaje) {

        // 🔥 Escudo protector: Si los campos están vacíos, avisa pero no explota
        if (apiUrl.isEmpty() || instancia.isEmpty() || token.isEmpty()) {
            System.out.println("⚠️ [LINDA MAMÁ SPA] Simulación de envío: Las credenciales de WhatsApp están vacías.");
            return;
        }

        String endpoint = apiUrl + "/message/sendText/" + instancia;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", token);

        Map<String, Object> body = new HashMap<>();

        // Limpiamos el número por si la recepcionista le puso espacios o símbolos
        String numeroLimpio = numeroDestino.replaceAll("[^0-9]", "");

        body.put("number", numeroLimpio);
        body.put("text", mensaje);

        Map<String, Object> options = new HashMap<>();
        options.put("delay", 1500);
        body.put("options", options);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            System.out.println("🌸 [LINDA MAMÁ SPA] Intentando disparar mensaje a: " + numeroLimpio);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ [LINDA MAMÁ SPA] ¡WhatsApp Enviado exitosamente!");
            } else {
                System.err.println("⚠️ [LINDA MAMÁ SPA] Evolution respondió con error. Código: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ [LINDA MAMÁ SPA] Error físico de conexión con Evolution API: " + e.getMessage());
        }
    }
}