package com.gomezsystems.minierp.service;

import com.gomezsystems.minierp.model.Tratamiento;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class SpaDataService {

    public List<Tratamiento> leerPacks() {
        return leerCsv("data/PACKS.csv");
    }

    public List<Tratamiento> leerSesiones() {
        return leerCsv("data/SESIONES.csv");
    }

    private List<Tratamiento> leerCsv(String ruta) {
        List<Tratamiento> lista = new ArrayList<>();
        try {
            // Leemos el archivo asegurando que usamos la codificación correcta
            Reader reader = new InputStreamReader(new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8);

            // Configuramos Apache Commons CSV para leer los encabezados
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);

            for (CSVRecord record : records) {
                Tratamiento t = new Tratamiento();

                // Mapeamos las columnas exactas de tus CSV
                t.setNombre(record.get("TRATAMIENTO"));
                t.setImagen(record.get("IMAGEN"));
                t.setDescripcion(record.get("DESCRIPCION"));

                // Limpiamos el precio por si viene con el signo $ u otros caracteres
                String precioStr = record.get("PRECIO SESION").replaceAll("[^0-9]", "");
                t.setPrecio(precioStr.isEmpty() ? 0 : Integer.parseInt(precioStr));

                lista.add(t);
            }
        } catch (Exception e) {
            System.err.println("Error leyendo el archivo " + ruta + ": " + e.getMessage());
        }
        return lista;
    }
}