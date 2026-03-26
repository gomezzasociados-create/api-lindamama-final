package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.repository.TratamientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @Autowired
    private TratamientoRepository tratamientoRepository;

    // 🏠 LA REDIRECCIÓN MAESTRA
    // Si alguien entra al link principal sin escribir nada, lo mandamos al catálogo de una.
    @GetMapping("/")
    public String home() {
        return "redirect:/catalogo";
    }

    // 🛍️ LA PUERTA DEL CATÁLOGO
    @GetMapping("/catalogo")
    public String mostrarCatalogo(Model model) {
        // Buscamos todos los tratamientos en la base de datos
        model.addAttribute("packs", tratamientoRepository.findAll());
        // Esto busca el archivo tratamientos.html en la carpeta templates
        return "tratamientos";
    }

    // 📅 LA PUERTA DE LA AGENDA
    @GetMapping({"/portal-reservas", "/agenda"})
    public String abrirAgenda() {
        // Esto busca el archivo agenda.html en la carpeta templates
        return "agenda";
    }
}