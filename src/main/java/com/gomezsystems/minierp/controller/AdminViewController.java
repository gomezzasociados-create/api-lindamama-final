package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.ClienteRepository;
import com.gomezsystems.minierp.repository.TratamientoRepository;
import com.gomezsystems.minierp.repository.EgresoRepository;
import com.gomezsystems.minierp.repository.CorteZRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminViewController {

    @Autowired private CitaRepository citaRepository;
    @Autowired private TratamientoRepository tratamientoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EgresoRepository egresoRepository;
    @Autowired private CorteZRepository corteZRepository;
    @Autowired private VentaRepository ventaRepository;

    // LA PUERTA PRINCIPAL DEL CENTRO DE MANDO
    @GetMapping("/admin")
    public String verPanelAdmin(Model model) {
        
        // Se revirtió el bloqueo estricto backend a petición del usuario.
        // Carga de datos MVC regular.



        try { model.addAttribute("citas", citaRepository.findAll()); } catch (Exception e) { model.addAttribute("citas", new ArrayList<>()); }
        try { model.addAttribute("tratamientos", tratamientoRepository.findAll()); } catch (Exception e) { model.addAttribute("tratamientos", new ArrayList<>()); }
        try { model.addAttribute("clientes", clienteRepository.findAll()); } catch (Exception e) { model.addAttribute("clientes", new ArrayList<>()); }
        
        // TESORERÍA (Se agregan los faltantes)
        try { model.addAttribute("egresos", egresoRepository.findAllByOrderByFechaHoraDesc()); } catch (Exception e) { model.addAttribute("egresos", new ArrayList<>()); }
        try { model.addAttribute("cortes", corteZRepository.findAllByOrderByFechaGeneracionDesc()); } catch (Exception e) { model.addAttribute("cortes", new ArrayList<>()); }
        
        try { 
            Double sumaVentas = ventaRepository.sumTotalPagadoByEstadoPagado();
            model.addAttribute("ingresosTotales", sumaVentas != null ? sumaVentas : 0.0);
        } catch (Exception e) {
            model.addAttribute("ingresosTotales", 0.0);
        }

        return "admin";
    }

    // LA PUERTA DEL POS
    @GetMapping("/pos")
    public String abrirPOS(Model model) {
        // 🔥 ESTO OBLIGA A MANDAR LOS PRODUCTOS SÍ O SÍ AL HTML
        model.addAttribute("servicios", tratamientoRepository.findAll());

        try {
            model.addAttribute("clientes", clienteRepository.findAll());
        } catch (Exception e) {
            System.err.println("Aviso POS: Aún no hay clientes en la BD.");
            model.addAttribute("clientes", new ArrayList<>());
        }
        return "pos";
    }
}