package com.gomezsystems.minierp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "egresos")
public class Egreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descripcion; // ¿En qué se gastó? Ej: "Pago de propina", "Compra toallas"
    private Double monto;       // ¿Cuánto salió de la caja?
    private String metodoSalida; // EFECTIVO o TRANSFERENCIA
    private boolean cierreAplicado = false; // ¿Ya se contabilizó en un Reporte Z?

    private LocalDateTime fechaHora = LocalDateTime.now();

    public Egreso() {
    }

    // =========================================
    // GETTERS Y SETTERS
    // =========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public String getMetodoSalida() { return metodoSalida; }
    public void setMetodoSalida(String metodoSalida) { this.metodoSalida = metodoSalida; }

    public boolean isCierreAplicado() { return cierreAplicado; }
    public void setCierreAplicado(boolean cierreAplicado) { this.cierreAplicado = cierreAplicado; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}