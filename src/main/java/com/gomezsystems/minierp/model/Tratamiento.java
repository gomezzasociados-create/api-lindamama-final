package com.gomezsystems.minierp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tratamiento")
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    // 🔥 AQUÍ ESTÁ LA MAGIA: Le decimos a la base de datos que este texto será ilimitado 🔥
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String imagen;
    private Integer precio;
    private String cantidad;

    // Constructor vacío (Obligatorio para Spring Boot)
    public Tratamiento() {
    }

    // =========================================
    // GETTERS Y SETTERS
    // =========================================

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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Integer getPrecio() {
        return precio;
    }

    public void setPrecio(Integer precio) {
        this.precio = precio;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }
}