package com.gomezsystems.minierp.config;

import com.gomezsystems.minierp.model.Tratamiento;
import com.gomezsystems.minierp.repository.TratamientoRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private TratamientoRepository tratamientoRepo;

    @Override
    public void run(String... args) throws Exception {
        if (tratamientoRepo.count() == 0) {
            System.out.println("🚀 [GOMEZ SYSTEMS] Base de datos vacía. Migrando datos desde CSV...");
            cargarDatos("data/PACKS.csv");
            cargarDatos("data/SESIONES.csv");
            System.out.println("✅ [GOMEZ SYSTEMS] ¡Catálogo cargado exitosamente en la base de datos!");
        } else {
            // Migrar tratamientos existentes sin categoría
            long sinCat = tratamientoRepo.findAll().stream().filter(t -> t.getCategoria() == null || t.getCategoria().isEmpty()).count();
            if (sinCat > 0) {
                System.out.println("🔄 [GOMEZ SYSTEMS] Detectados " + sinCat + " tratamientos sin categoría. Clasificando...");
                for (Tratamiento t : tratamientoRepo.findAll()) {
                    if (t.getCategoria() == null || t.getCategoria().isEmpty()) {
                        String nom = t.getNombre() != null ? t.getNombre().toLowerCase() : "";
                        String dsc = t.getDescripcion() != null ? t.getDescripcion().toLowerCase() : "";
                        if (nom.contains("facial") || dsc.contains("facial")) {
                            t.setCategoria("faciales");
                        } else if (nom.contains("relaj") || dsc.contains("relaj")) {
                            t.setCategoria("relajantes");
                        } else if (nom.contains("pack") || dsc.contains("pack")) {
                            t.setCategoria("pos-operatorios");
                        } else {
                            t.setCategoria("reductivos");
                        }
                        tratamientoRepo.save(t);
                    }
                }
                System.out.println("✅ [GOMEZ SYSTEMS] ¡Clasificación automática completada!");
            }
        }
    }

    private void cargarDatos(String ruta) {
        try {
            Reader reader = new InputStreamReader(new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8);

            // 🔥 MODO BLINDADO: Ignora mayúsculas/minúsculas y borra espacios en blanco extra de los títulos
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .parse(reader);

            for (CSVRecord record : records) {
                Tratamiento t = new Tratamiento();

                // Usamos isMapped para que NO explote si una columna no existe
                t.setNombre(record.isMapped("TRATAMIENTO") ? record.get("TRATAMIENTO") : "Sin Nombre");
                t.setImagen(record.isMapped("IMAGEN") ? record.get("IMAGEN") : "");
                t.setDescripcion(record.isMapped("DESCRIPCION") ? record.get("DESCRIPCION") : "");

                // Protección para el precio
                if (record.isMapped("PRECIO SESION")) {
                    String precioStr = record.get("PRECIO SESION").replaceAll("[^0-9]", "");
                    t.setPrecio(precioStr.isEmpty() ? 0 : Integer.parseInt(precioStr));
                } else {
                    t.setPrecio(0);
                }

                // Protección para la cantidad
                if (record.isMapped("CANTIDAD SESIONES")) {
                    t.setCantidad(record.get("CANTIDAD SESIONES"));
                } else if (record.isMapped("CANTIDAD DE SESIONES")) {
                    t.setCantidad(record.get("CANTIDAD DE SESIONES") + " sesión(es)");
                } else {
                    t.setCantidad("1 sesión");
                }

                // Clasificación automática por palabras clave
                String nom = t.getNombre() != null ? t.getNombre().toLowerCase() : "";
                String dsc = t.getDescripcion() != null ? t.getDescripcion().toLowerCase() : "";
                if (nom.contains("facial") || dsc.contains("facial")) {
                    t.setCategoria("faciales");
                } else if (nom.contains("relaj") || dsc.contains("relaj")) {
                    t.setCategoria("relajantes");
                } else if (nom.contains("pack") || dsc.contains("pack")) {
                    t.setCategoria("pos-operatorios");
                } else {
                    t.setCategoria("reductivos");
                }

                tratamientoRepo.save(t);
            }
        } catch (Exception e) {
            // 🔥 EL CHISMOSO: Ahora sí nos dirá la verdad absoluta de por qué falla
            System.err.println("⚠️ ERROR REAL leyendo el archivo " + ruta + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}