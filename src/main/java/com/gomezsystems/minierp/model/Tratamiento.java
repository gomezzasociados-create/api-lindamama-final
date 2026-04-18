package com.gomezsystems.minierp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "tratamiento")
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String imagen;
    private Integer precio;
    
    @Column(columnDefinition = "TEXT")
    private String cantidad;
    
    private Integer duracionMinutos; // Añadido para la agenda

    // 🔥 PROMOCIONES Y FECHAS DE CADUCIDAD 🔥
    private boolean enPromocion;
    private Integer precioPromo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicioPromo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFinPromo;

    public Tratamiento() {
    }

    // 🔥 MAGIA: ESTE MÉTODO CALCULA SI LA PROMO ESTÁ VIGENTE HOY 🔥
    public boolean isPromoActiva() {
        if (!this.enPromocion) return false; // Si el switch está apagado, chao promo

        LocalDate hoy = LocalDate.now();
        // Si hay fecha de inicio y hoy es ANTES de esa fecha, no la muestra
        if (this.fechaInicioPromo != null && hoy.isBefore(this.fechaInicioPromo)) return false;
        // Si hay fecha de fin y hoy es DESPUÉS de esa fecha, la desactiva (caducó)
        if (this.fechaFinPromo != null && hoy.isAfter(this.fechaFinPromo)) return false;

        return true; // Si pasa todos los filtros, la promo se muestra
    }

    // =========================================
    // GETTERS Y SETTERS
    // =========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public Integer getPrecio() { return precio; }
    public void setPrecio(Integer precio) { this.precio = precio; }
    public String getCantidad() { return cantidad; }
    public void setCantidad(String cantidad) { this.cantidad = cantidad; }

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public boolean isEnPromocion() { return enPromocion; }
    public void setEnPromocion(boolean enPromocion) { this.enPromocion = enPromocion; }
    public Integer getPrecioPromo() { return precioPromo; }
    public void setPrecioPromo(Integer precioPromo) { this.precioPromo = precioPromo; }
    public LocalDate getFechaInicioPromo() { return fechaInicioPromo; }
    public void setFechaInicioPromo(LocalDate fechaInicioPromo) { this.fechaInicioPromo = fechaInicioPromo; }
    public LocalDate getFechaFinPromo() { return fechaFinPromo; }
    public void setFechaFinPromo(LocalDate fechaFinPromo) { this.fechaFinPromo = fechaFinPromo; }
}