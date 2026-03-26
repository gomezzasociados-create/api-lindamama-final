package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Cliente;
import com.gomezsystems.minierp.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private ClienteRepository clienteRepository;

    // 1. Obtener todos los clientes
    @GetMapping
    public List<Cliente> obtenerClientes() {
        return clienteRepository.findAll();
    }

    // 2. Guardar o Actualizar (Coincide con /api/clientes/guardar)
    @PostMapping("/guardar")
    public Cliente guardarCliente(@RequestBody Cliente nuevoCliente) {
        return clienteRepository.save(nuevoCliente);
    }

    // 3. Borrar (Coincide con /api/clientes/borrar/{id})
    @DeleteMapping("/borrar/{id}")
    public ResponseEntity<String> borrarCliente(@PathVariable Long id) {
        if (clienteRepository.existsById(id)) {
            clienteRepository.deleteById(id);
            return ResponseEntity.ok("Cliente eliminado con éxito");
        }
        return ResponseEntity.badRequest().body("No se encontró el cliente");
    }
}