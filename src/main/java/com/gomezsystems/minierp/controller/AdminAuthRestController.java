package com.gomezsystems.minierp.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AdminAuthRestController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload, HttpSession session) {
        String pin = payload.get("pin");
        
        if ("7777".equals(pin)) {
            session.setAttribute("ADMIN_USER", "Crisley Paredes");
            session.setAttribute("ADMIN_ROL", "PROPIETARIA");
            return ResponseEntity.ok(Map.of("status", "ok", "user", "Crisley Paredes"));
        } else if ("1010".equals(pin)) {
            session.setAttribute("ADMIN_USER", "Janneth");
            session.setAttribute("ADMIN_ROL", "ADMINISTRADORA");
            return ResponseEntity.ok(Map.of("status", "ok", "user", "Janneth"));
        }

        return ResponseEntity.status(401).body(Map.of("status", "error", "message", "PIN Inválido"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
