package com.gomezsystems.minierp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String telefono;
    private String correo;
    private String cumpleanos; // Guardaremos algo como "15 de Mayo" o "1990-05-15"

    @Column(columnDefinition = "TEXT")
    private String notasMedicas;

    @Column(columnDefinition = "TEXT")
    private String historialCompras; // Aquí anotaremos qué ha comprado o datos clave

    public Cliente() {}

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCumpleanos() { return cumpleanos; }
    public void setCumpleanos(String cumpleanos) { this.cumpleanos = cumpleanos; }

    public String getNotasMedicas() { return notasMedicas; }
    public void setNotasMedicas(String notasMedicas) { this.notasMedicas = notasMedicas; }

    public String getHistorialCompras() { return historialCompras; }
    public void setHistorialCompras(String historialCompras) { this.historialCompras = historialCompras; }
}