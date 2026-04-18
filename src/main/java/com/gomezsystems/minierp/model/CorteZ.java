package com.gomezsystems.minierp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cortez")
public class CorteZ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // INGRESOS
    private Double totalEfectivo;
    private Double totalFiado;
    private Double totalTarjeta;
    private Double totalTransferencia;

    // SALIDAS
    private Double totalEgresos;

    // RESULTADO FINAL (Lo que debe haber en la gaveta física)
    private Double efectivoEsperadoCajon;

    @Column(columnDefinition = "TEXT")
    private String detalleInforme;

    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    public CorteZ() {
    }

    // =========================================
    // GETTERS Y SETTERS
    // =========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getTotalEfectivo() { return totalEfectivo; }
    public void setTotalEfectivo(Double totalEfectivo) { this.totalEfectivo = totalEfectivo; }

    public Double getTotalFiado() { return totalFiado; }
    public void setTotalFiado(Double totalFiado) { this.totalFiado = totalFiado; }

    public Double getTotalTarjeta() { return totalTarjeta; }
    public void setTotalTarjeta(Double totalTarjeta) { this.totalTarjeta = totalTarjeta; }

    public Double getTotalTransferencia() { return totalTransferencia; }
    public void setTotalTransferencia(Double totalTransferencia) { this.totalTransferencia = totalTransferencia; }

    public Double getTotalEgresos() { return totalEgresos; }
    public void setTotalEgresos(Double totalEgresos) { this.totalEgresos = totalEgresos; }

    public Double getEfectivoEsperadoCajon() { return efectivoEsperadoCajon; }
    public void setEfectivoEsperadoCajon(Double efectivoEsperadoCajon) { this.efectivoEsperadoCajon = efectivoEsperadoCajon; }

    public String getDetalleInforme() { return detalleInforme; }
    public void setDetalleInforme(String detalleInforme) { this.detalleInforme = detalleInforme; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}