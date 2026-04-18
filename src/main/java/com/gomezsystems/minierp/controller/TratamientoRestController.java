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

    // 2. RUTA PARA ELIMINAR INDIVIDUALMENTE
    @DeleteMapping("/borrar/{id}")
    public ResponseEntity<String> borrarTratamiento(@PathVariable Long id) {
        tratamientoRepo.deleteById(id);
        return ResponseEntity.ok("Servicio eliminado correctamente");
    }

    // 2.5 RUTA PARA BORRADO MASIVO
    @DeleteMapping("/borrar-todos")
    public ResponseEntity<String> borrarTodosLosTratamientos() {
        tratamientoRepo.deleteAll();
        return ResponseEntity.ok("Se ha borrado todo el catálogo de servicios.");
    }

    // 3. RUTA PARA SUBIDA MASIVA (CSV)
    @PostMapping("/upload-csv")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo está vacío");
        }
        try {
            java.io.Reader reader = new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
            Iterable<org.apache.commons.csv.CSVRecord> records = org.apache.commons.csv.CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .parse(reader);

            int count = 0;
            for (org.apache.commons.csv.CSVRecord record : records) {
                // If ID is mapped and exists, we avoid inserting row
                Tratamiento t = new Tratamiento();

                t.setNombre(record.isMapped("TRATAMIENTO") ? record.get("TRATAMIENTO") : "Sin Nombre");
                t.setImagen(record.isMapped("IMAGEN") ? record.get("IMAGEN") : "");
                t.setDescripcion(record.isMapped("DESCRIPCION") ? record.get("DESCRIPCION") : "");

                if (record.isMapped("PRECIO SESION")) {
                    String precioStr = record.get("PRECIO SESION").replaceAll("[^0-9]", "");
                    t.setPrecio(precioStr.isEmpty() ? 0 : Integer.parseInt(precioStr));
                } else {
                    t.setPrecio(0);
                }

                if (record.isMapped("CANTIDAD SESIONES")) {
                    t.setCantidad(record.get("CANTIDAD SESIONES"));
                } else if (record.isMapped("CANTIDAD DE SESIONES")) {
                    t.setCantidad(record.get("CANTIDAD DE SESIONES") + " sesión(es)");
                } else {
                    t.setCantidad("1 sesión");
                }

                tratamientoRepo.save(t);
                count++;
            }
            return ResponseEntity.ok("Se han importado " + count + " servicios exitosamente.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error al procesar el archivo CSV: " + e.getMessage());
        }
    }
}