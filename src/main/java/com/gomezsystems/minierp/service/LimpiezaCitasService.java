package com.gomezsystems.minierp.service;

import com.gomezsystems.minierp.model.Cita;
import com.gomezsystems.minierp.model.Venta;
import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LimpiezaCitasService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    // 🔥 Este reloj interno hace que el método se ejecute cada 60,000 milisegundos (1 minuto)
    @Scheduled(fixedRate = 60000)
    public void limpiarReservasVencidas() {

        // 1. Calculamos exactamente qué hora era hace 10 minutos
        LocalDateTime limite = LocalDateTime.now().minusMinutes(10);

        // 2. Usamos el método mágico que creamos en el Paso 3
        List<Cita> citasVencidas = citaRepository.findByEstadoAndFechaCreacionBefore("RESERVADO", limite);
        List<Venta> ventasVencidas = ventaRepository.findByEstadoAndFechaHoraBefore("RESERVADO", limite);

        // 3. Si encuentra citas abandonadas, las elimina para liberar tu agenda
        if (!citasVencidas.isEmpty()) {
            citaRepository.deleteAll(citasVencidas);
            System.out.println("🧹 Limpieza Gomez Systems: Se liberaron " + citasVencidas.size() + " cupos no pagados a tiempo.");
        }

        // 4. Limpiamos las ventas falsas para que no generen comisiones incorrectas a las socias
        if (!ventasVencidas.isEmpty()) {
            ventaRepository.deleteAll(ventasVencidas);
            System.out.println("🧹 Limpieza Gomez Systems: Se eliminaron " + ventasVencidas.size() + " ventas fantasma por reservas no pagadas.");
        }
    }
}