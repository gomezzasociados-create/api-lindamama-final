package com.gomezsystems.minierp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- DATOS DEL COBRO ---
    private Double montoTotal;   // Lo que cuesta el servicio/producto
    private Double propina;      // Propina extra para el terapeuta
    private Double totalPagado;  // Suma de (montoTotal + propina)

    @Column(columnDefinition = "TEXT")
    private String detalle;      // Resumen en texto del ticket

    private String tipoPago;     // EFECTIVO, TARJETA, TRANSFERENCIA, FIADO o PAQUETE
    private String estado;       // PAGADO, PENDIENTE
    private boolean cierreAplicado = false; // Para el Reporte Z
    private boolean liquidadoSocia = false; // Para pago de comisiones a la profesional

    // --- DATOS DEL SPA Y COMISIONES ---
    @ManyToOne
    @JoinColumn(name = "socia_id")
    private Socia socia; // La profesional que hizo el servicio

    private Double montoSpa = 0.0;
    private Double montoSocia = 0.0;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "tratamiento_id")
    private Tratamiento tratamiento; // Qué servicio específico se realizó

    @ManyToOne
    @JoinColumn(name = "paquete_origen_id")
    private PaqueteComprado paqueteOrigen; // Si no pagó con dinero hoy, sino usando su paquete

    private LocalDateTime fechaHora = LocalDateTime.now();

    // Constructor vacío requerido por Spring/Hibernate
    public Venta() {
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(Double montoTotal) {
        this.montoTotal = montoTotal;
        calcularTotalPagado();
    }

    public Double getPropina() {
        return propina;
    }

    public void setPropina(Double propina) {
        this.propina = propina;
        calcularTotalPagado();
    }

    public Double getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(Double totalPagado) {
        this.totalPagado = totalPagado;
    }

    // Método auxiliar para mantener siempre cuadrado el total
    private void calcularTotalPagado() {
        double m = (montoTotal != null) ? montoTotal : 0.0;
        double p = (propina != null) ? propina : 0.0;
        this.totalPagado = m + p;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isCierreAplicado() {
        return cierreAplicado;
    }

    public void setCierreAplicado(boolean cierreAplicado) {
        this.cierreAplicado = cierreAplicado;
    }

    public Socia getSocia() {
        return socia;
    }

    public void setSocia(Socia socia) {
        this.socia = socia;
    }

    public Double getMontoSpa() {
        return montoSpa;
    }

    public void setMontoSpa(Double montoSpa) {
        this.montoSpa = montoSpa;
    }

    public Double getMontoSocia() {
        return montoSocia;
    }

    public void setMontoSocia(Double montoSocia) {
        this.montoSocia = montoSocia;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Tratamiento getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(Tratamiento tratamiento) {
        this.tratamiento = tratamiento;
    }

    public PaqueteComprado getPaqueteOrigen() {
        return paqueteOrigen;
    }

    public void setPaqueteOrigen(PaqueteComprado paqueteOrigen) {
        this.paqueteOrigen = paqueteOrigen;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public boolean isLiquidadoSocia() {
        return liquidadoSocia;
    }

    public void setLiquidadoSocia(boolean liquidadoSocia) {
        this.liquidadoSocia = liquidadoSocia;
    }
}