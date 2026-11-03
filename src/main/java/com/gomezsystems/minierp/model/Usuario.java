package com.gomezsystems.minierp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true)
    private String username;

    private String pin; // Clave / PIN privado asignado

    private String rol; // PROPIETARIA, ADMINISTRADORA, CAJERA, RECEPCION

    private boolean accesoAdmin = true;
    private boolean accesoPos = true;
    private boolean activo = true;

    private LocalDateTime fechaCreacion;

    public Usuario() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public Usuario(String nombre, String username, String pin, String rol, boolean accesoAdmin, boolean accesoPos) {
        this.nombre = nombre;
        this.username = username;
        this.pin = pin;
        this.rol = rol;
        this.accesoAdmin = accesoAdmin;
        this.accesoPos = accesoPos;
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isAccesoAdmin() {
        return accesoAdmin;
    }

    public void setAccesoAdmin(boolean accesoAdmin) {
        this.accesoAdmin = accesoAdmin;
    }

    public boolean isAccesoPos() {
        return accesoPos;
    }

    public void setAccesoPos(boolean accesoPos) {
        this.accesoPos = accesoPos;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
