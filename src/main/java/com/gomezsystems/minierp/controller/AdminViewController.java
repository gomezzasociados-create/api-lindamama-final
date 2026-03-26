package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.ClienteRepository;
import com.gomezsystems.minierp.repository.TratamientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;

@Controller
public class AdminViewController {

    @Autowired private CitaRepository citaRepository;
    @Autowired private TratamientoRepository tratamientoRepository;
    @Autowired private ClienteRepository clienteRepository;

    // LA PUERTA PRINCIPAL DEL CENTRO DE MANDO
    @GetMapping("/admin")
    public String verPanelAdmin(Model model) {
        try {
            // Intentamos traer los datos reales
            model.addAttribute("citas", citaRepository.findAll());
            model.addAttribute("tratamientos", tratamientoRepository.findAll());
            model.addAttribute("clientes", clienteRepository.findAll());
        } catch (Exception e) {
            // Modo seguro: si la BD falla, mandamos listas vacías
            System.err.println("Error cargando BD en Admin: " + e.getMessage());
            model.addAttribute("citas", new ArrayList<>());
            model.addAttribute("tratamientos", new ArrayList<>());
            model.addAttribute("clientes", new ArrayList<>());
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