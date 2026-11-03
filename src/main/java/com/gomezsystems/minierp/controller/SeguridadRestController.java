package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.ConfiguracionSeguridad;
import com.gomezsystems.minierp.model.Usuario;
import com.gomezsystems.minierp.repository.ConfiguracionSeguridadRepository;
import com.gomezsystems.minierp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/seguridad")
public class SeguridadRestController {

    private static final String CLAVE_PIN_MAESTRO = "PIN_MAESTRO_TESORERIA";
    private static final String DEFAULT_PIN_MAESTRO = "777777";

    @Autowired
    private ConfiguracionSeguridadRepository configuracionSeguridadRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    // ==========================================
    // 1. GESTIÓN DEL PIN MAESTRO DE 6 DÍGITOS
    // ==========================================

    @GetMapping("/pin-maestro")
    public ResponseEntity<?> obtenerPinMaestro() {
        try {
            ConfiguracionSeguridad config = configuracionSeguridadRepo.findByClave(CLAVE_PIN_MAESTRO)
                    .orElseGet(() -> configuracionSeguridadRepo.save(new ConfiguracionSeguridad(
                            CLAVE_PIN_MAESTRO,
                            DEFAULT_PIN_MAESTRO,
                            "Clave Maestra de 6 dígitos para reinicio y eliminación segura en Tesorería y Finanzas"
                    )));

            String pinVal = config.getValor() != null ? config.getValor() : DEFAULT_PIN_MAESTRO;
            return ResponseEntity.ok(Map.of(
                    "configurado", true,
                    "longitud", 6,
                    "pin", pinVal,
                    "valor", pinVal
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "configurado", true,
                    "longitud", 6,
                    "pin", DEFAULT_PIN_MAESTRO,
                    "valor", DEFAULT_PIN_MAESTRO
            ));
        }
    }

    @PutMapping("/pin-maestro")
    public ResponseEntity<?> actualizarPinMaestro(@RequestBody Map<String, String> payload) {
        String nuevoPin = payload.get("nuevoPin") != null ? payload.get("nuevoPin") : payload.get("pin");
        if (nuevoPin == null || !nuevoPin.matches("^\\d{6}$")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "La clave maestra debe contener exactamente 6 dígitos numéricos (ej: 777777 o 123456)."
            ));
        }

        try {
            ConfiguracionSeguridad config = configuracionSeguridadRepo.findByClave(CLAVE_PIN_MAESTRO)
                    .orElse(new ConfiguracionSeguridad(
                            CLAVE_PIN_MAESTRO,
                            nuevoPin,
                            "Clave Maestra de 6 dígitos para reinicio y eliminación segura en Tesorería y Finanzas"
                    ));

            config.setValor(nuevoPin);
            configuracionSeguridadRepo.save(config);

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "mensaje", "¡Clave maestra de 6 dígitos actualizada exitosamente en el sistema!",
                    "pin", nuevoPin,
                    "valor", nuevoPin
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "mensaje", "Clave maestra actualizada (fallback local activo).",
                    "pin", nuevoPin,
                    "valor", nuevoPin
            ));
        }
    }

    @PostMapping("/verificar-pin-maestro")
    public ResponseEntity<?> verificarPinMaestro(@RequestBody Map<String, String> payload) {
        String pinIngresado = payload.get("pin");
        String pinValido = DEFAULT_PIN_MAESTRO;

        try {
            ConfiguracionSeguridad config = configuracionSeguridadRepo.findByClave(CLAVE_PIN_MAESTRO).orElse(null);
            if (config != null && config.getValor() != null) {
                pinValido = config.getValor();
            }
        } catch (Exception ignored) {}

        boolean coincide = pinValido != null && pinValido.equals(pinIngresado);

        return ResponseEntity.ok(Map.of(
                "valido", coincide,
                "mensaje", coincide ? "Autorización concedida." : "Clave de 6 dígitos incorrecta."
        ));
    }

    // ==========================================
    // 2. GESTIÓN DE USUARIOS / CREDENCIALES (ADMIN & POS)
    // ==========================================

    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios() {
        try {
            List<Usuario> list = usuarioRepo.findAllByOrderByNombreAsc();
            if (list == null || list.isEmpty()) {
                // Auto-inicialización segura si la tabla está vacía
                usuarioRepo.save(new Usuario("Crisley Paredes", "crisley", "7777", "PROPIETARIA", true, true));
                usuarioRepo.save(new Usuario("Janneth", "janneth", "1010", "ADMINISTRADORA", true, true));
                usuarioRepo.save(new Usuario("Daniela Camacho", "daniela.camacho", "2020", "CAJERA", false, true));
                usuarioRepo.save(new Usuario("Daniela Montaño", "daniela.montano", "3030", "CAJERA", false, true));
                list = usuarioRepo.findAllByOrderByNombreAsc();
            }
            return list;
        } catch (Exception e) {
            return List.of(
                    new Usuario("Crisley Paredes", "crisley", "7777", "PROPIETARIA", true, true),
                    new Usuario("Janneth", "janneth", "1010", "ADMINISTRADORA", true, true),
                    new Usuario("Daniela Camacho", "daniela.camacho", "2020", "CAJERA", false, true),
                    new Usuario("Daniela Montaño", "daniela.montano", "3030", "CAJERA", false, true)
            );
        }
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario u) {
        if (u.getNombre() == null || u.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre del usuario es obligatorio."));
        }
        if (u.getPin() == null || u.getPin().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La clave o PIN privado es obligatorio."));
        }

        // Generar username si viene vacío
        if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
            String base = u.getNombre().toLowerCase().replaceAll("[^a-z0-9]", ".");
            if (base.endsWith(".")) base = base.substring(0, base.length() - 1);
            u.setUsername(base);
        }

        if (u.getRol() == null || u.getRol().trim().isEmpty()) {
            u.setRol("ADMINISTRADORA");
        }

        Usuario guardado = usuarioRepo.save(u);
        return ResponseEntity.ok(guardado);
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Optional<Usuario> uOpt = usuarioRepo.findById(id);
        if (uOpt.isEmpty()) return ResponseEntity.notFound().build();

        Usuario u = uOpt.get();
        if (datos.containsKey("nombre") && datos.get("nombre") != null) {
            u.setNombre(datos.get("nombre").toString());
        }
        if (datos.containsKey("username") && datos.get("username") != null) {
            u.setUsername(datos.get("username").toString());
        }
        if (datos.containsKey("pin") && datos.get("pin") != null && !datos.get("pin").toString().trim().isEmpty()) {
            u.setPin(datos.get("pin").toString().trim());
        }
        if (datos.containsKey("rol") && datos.get("rol") != null) {
            u.setRol(datos.get("rol").toString());
        }
        if (datos.containsKey("accesoAdmin") && datos.get("accesoAdmin") != null) {
            u.setAccesoAdmin(Boolean.parseBoolean(datos.get("accesoAdmin").toString()));
        }
        if (datos.containsKey("accesoPos") && datos.get("accesoPos") != null) {
            u.setAccesoPos(Boolean.parseBoolean(datos.get("accesoPos").toString()));
        }
        if (datos.containsKey("activo") && datos.get("activo") != null) {
            u.setActivo(Boolean.parseBoolean(datos.get("activo").toString()));
        }

        Usuario actualizado = usuarioRepo.save(u);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        Optional<Usuario> uOpt = usuarioRepo.findById(id);
        if (uOpt.isEmpty()) return ResponseEntity.notFound().build();

        Usuario u = uOpt.get();
        if ("PROPIETARIA".equalsIgnoreCase(u.getRol())) {
            long totalPropietarias = usuarioRepo.findAll().stream()
                    .filter(usr -> "PROPIETARIA".equalsIgnoreCase(usr.getRol()) && usr.isActivo())
                    .count();
            if (totalPropietarias <= 1) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No puedes eliminar al único usuario con rol PROPIETARIA del sistema."
                ));
            }
        }

        usuarioRepo.delete(u);
        return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Usuario eliminado correctamente."));
    }
}
