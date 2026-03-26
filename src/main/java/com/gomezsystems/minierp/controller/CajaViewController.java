package com.gomezsystems.minierp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CajaViewController {

    // Esta es la ruta que le faltaba a tu servidor
    @GetMapping("/ventas")
    public String mostrarPantallaVentas() {
        return "ventas"; // Esto le dice que busque y muestre ventas.html
    }
}