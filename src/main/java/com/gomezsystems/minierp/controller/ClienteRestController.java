package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Cliente;
import com.gomezsystems.minierp.repository.ClienteRepository;
import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    // 1. Obtener todos los clientes
    @GetMapping
    public List<Cliente> obtenerClientes() {
        return clienteRepository.findAll();
    }

    // 2. Guardar o Actualizar (Coincide con /api/clientes/guardar)
    // Conserva kardexJson y demás datos si no vienen en el payload
    @PostMapping("/guardar")
    public Cliente guardarCliente(@RequestBody Cliente nuevoCliente) {
        if (nuevoCliente.getId() != null) {
            Cliente existente = clienteRepository.findById(nuevoCliente.getId()).orElse(null);
            if (existente != null) {
                if (nuevoCliente.getNombre() != null && !nuevoCliente.getNombre().trim().isEmpty()) {
                    existente.setNombre(nuevoCliente.getNombre().trim());
                }
                if (nuevoCliente.getTelefono() != null) {
                    existente.setTelefono(nuevoCliente.getTelefono());
                }
                if (nuevoCliente.getCorreo() != null) {
                    existente.setCorreo(nuevoCliente.getCorreo());
                }
                if (nuevoCliente.getCumpleanos() != null) {
                    existente.setCumpleanos(nuevoCliente.getCumpleanos());
                }
                if (nuevoCliente.getNotasMedicas() != null) {
                    existente.setNotasMedicas(nuevoCliente.getNotasMedicas());
                }
                if (nuevoCliente.getHistorialCompras() != null) {
                    existente.setHistorialCompras(nuevoCliente.getHistorialCompras());
                }
                if (nuevoCliente.getKardexJson() != null && !nuevoCliente.getKardexJson().trim().isEmpty()) {
                    existente.setKardexJson(nuevoCliente.getKardexJson());
                }
                if (nuevoCliente.getDeudaActiva() != null) {
                    existente.setDeudaActiva(nuevoCliente.getDeudaActiva());
                }
                return clienteRepository.save(existente);
            }
        }
        return clienteRepository.save(nuevoCliente);
    }

    // 3. Borrar (Coincide con /api/clientes/borrar/{id})
    @DeleteMapping("/borrar/{id}")
    @Transactional
    public ResponseEntity<String> borrarCliente(@PathVariable Long id) {
        if (clienteRepository.existsById(id)) {
            citaRepository.desasociarCliente(id);
            ventaRepository.desasociarCliente(id);
            clienteRepository.deleteById(id);
            return ResponseEntity.ok("Cliente eliminado con éxito");
        }
        return ResponseEntity.badRequest().body("No se encontró el cliente");
    }
}
