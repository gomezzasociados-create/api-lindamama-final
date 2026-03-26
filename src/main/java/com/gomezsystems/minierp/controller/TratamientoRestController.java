package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Tratamiento;
import com.gomezsystems.minierp.repository.TratamientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tratamientos")
public class TratamientoRestController {

    @Autowired
    private TratamientoRepository tratamientoRepo;

    // 1. RUTA PARA GUARDAR O EDITAR
    @PostMapping("/guardar")
    public ResponseEntity<Tratamiento> guardarTratamiento(@RequestBody Tratamiento tratamiento) {
        // El .save() hace la magia: si no tiene ID lo crea, si tiene ID lo actualiza
        Tratamiento guardado = tratamientoRepo.save(tratamiento);
        return ResponseEntity.ok(guardado);
    }

    // 2. RUTA PARA ELIMINAR
    @DeleteMapping("/borrar/{id}")
    public ResponseEntity<String> borrarTratamiento(@PathVariable Long id) {
        tratamientoRepo.deleteById(id);
        return ResponseEntity.ok("Servicio eliminado correctamente");
    }
}