package com.gomezsystems.minierp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_comisiones")
public class PagoComision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaHora = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "socia_id")
    private Socia socia;

    private Double monto;
    private String medioPago; // Efectivo, Transferencia, etc.
    private String comentarios;

    public PagoComision() {}

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Socia getSocia() {
        return socia;
    }
    public void setSocia(Socia socia) {
        this.socia = socia;
    }

    public Double getMonto() {
        return monto;
    }
    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public String getMedioPago() {
        return medioPago;
    }
    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getComentarios() {
        return comentarios;
    }
    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }
}
