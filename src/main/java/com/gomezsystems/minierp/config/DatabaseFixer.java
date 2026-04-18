package com.gomezsystems.minierp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("---- INICIANDO AUTO-CORRECCIÓN DE BASE DE DATOS ----");
        
        try {
            jdbcTemplate.execute("ALTER TABLE ventas ADD COLUMN liquidado_socia boolean DEFAULT false");
            System.out.println("✅ Columna 'liquidado_socia' agregada con éxito.");
        } catch (Exception e) {
            System.out.println("ℹ️ Columna 'liquidado_socia' ya existe o no se pudo agregar.");
        }
        
        try {
            jdbcTemplate.execute("ALTER TABLE ventas ADD COLUMN monto_socia double precision DEFAULT 0.0");
            System.out.println("✅ Columna 'monto_socia' agregada con éxito.");
        } catch (Exception e) {
            System.out.println("ℹ️ Columna 'monto_socia' ya existe.");
        }

        try {
            jdbcTemplate.execute("ALTER TABLE ventas ADD COLUMN monto_spa double precision DEFAULT 0.0");
            System.out.println("✅ Columna 'monto_spa' agregada con éxito.");
        } catch (Exception e) {
            System.out.println("ℹ️ Columna 'monto_spa' ya existe.");
        }
        
        System.out.println("---- AUTO-CORRECCIÓN FINALIZADA ----");
    }
}
