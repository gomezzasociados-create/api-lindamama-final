package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Usuario;
import com.gomezsystems.minierp.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AdminAuthRestController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload, HttpSession session) {
        String pin = payload.get("pin");
        String origen = payload.getOrDefault("origen", "ADMIN").toUpperCase();

        if (pin == null || pin.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Debe ingresar una clave o PIN."));
        }

        pin = pin.trim();

        // 1. Buscar usuario activo en base de datos
        Optional<Usuario> uOpt = usuarioRepo.findByPinAndActivoTrue(pin);

        if (uOpt.isPresent()) {
            Usuario u = uOpt.get();

            // Validar acceso según origen
            if ("ADMIN".equals(origen) && !u.isAccesoAdmin()) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "error",
                        "message", "Acceso denegado: este usuario no tiene permisos asignados para el Centro de Mando (Admin)."
                ));
            }

            if ("POS".equals(origen) && !u.isAccesoPos()) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "error",
                        "message", "Acceso denegado: este usuario no tiene permisos asignados para el Punto de Venta (POS)."
                ));
            }

            session.setAttribute("ADMIN_USER", u.getNombre());
            session.setAttribute("ADMIN_ROL", u.getRol());

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "user", u.getNombre(),
                    "username", u.getUsername() != null ? u.getUsername() : "",
                    "rol", u.getRol(),
                    "accesoAdmin", u.isAccesoAdmin(),
                    "accesoPos", u.isAccesoPos()
            ));
        }

        // 2. Respaldo por defecto por si aún no se inicializó la base de datos
        if ("7777".equals(pin)) {
            session.setAttribute("ADMIN_USER", "Crisley Paredes");
            session.setAttribute("ADMIN_ROL", "PROPIETARIA");
            return ResponseEntity.ok(Map.of("status", "ok", "user", "Crisley Paredes", "rol", "PROPIETARIA", "accesoAdmin", true, "accesoPos", true));
        } else if ("1010".equals(pin)) {
            session.setAttribute("ADMIN_USER", "Janneth");
            session.setAttribute("ADMIN_ROL", "ADMINISTRADORA");
            return ResponseEntity.ok(Map.of("status", "ok", "user", "Janneth", "rol", "ADMINISTRADORA", "accesoAdmin", true, "accesoPos", true));
        } else if ("2020".equals(pin)) {
            session.setAttribute("ADMIN_USER", "Daniela Camacho");
            session.setAttribute("ADMIN_ROL", "CAJERA");
            return ResponseEntity.ok(Map.of("status", "ok", "user", "Daniela Camacho", "rol", "CAJERA", "accesoAdmin", false, "accesoPos", true));
        } else if ("3030".equals(pin)) {
            session.setAttribute("ADMIN_USER", "Daniela Montaño");
            session.setAttribute("ADMIN_ROL", "CAJERA");
            return ResponseEntity.ok(Map.of("status", "ok", "user", "Daniela Montaño", "rol", "CAJERA", "accesoAdmin", false, "accesoPos", true));
        }

        return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Clave o PIN no autorizado."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
