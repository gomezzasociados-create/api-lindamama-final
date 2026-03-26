package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Cita;
import com.gomezsystems.minierp.repository.CitaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaRestController {

    @Autowired
    private CitaRepository citaRepository;

    @GetMapping
    public List<Cita> listarCitas() {
        return citaRepository.findAll();
    }

    @PostMapping
    public Cita guardarCita(@RequestBody Cita cita) {
        cita.setEstado("AGENDADA");
        return citaRepository.save(cita);
    }
}