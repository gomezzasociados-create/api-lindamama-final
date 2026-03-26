package com.gomezsystems.minierp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "paquetes_comprados")
public class PaqueteComprado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private String nombreTratamiento;
    private int totalSesiones;
    private int sesionesRestantes;

    private double montoTotal;
    private double montoAbonado;
    private boolean estadoActivo;

    // NUEVOS CAMPOS PARA SEGUIMIENTO DE PAGO
    private String statusPago; // "PENDIENTE", "PAGADO"
    private String orderId;    // ID único para identificar esta compra

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public String getNombreTratamiento() { return nombreTratamiento; }
    public void setNombreTratamiento(String nombreTratamiento) { this.nombreTratamiento = nombreTratamiento; }

    public int getTotalSesiones() { return totalSesiones; }
    public void setTotalSesiones(int totalSesiones) { this.totalSesiones = totalSesiones; }

    public int getSesionesRestantes() { return sesionesRestantes; }
    public void setSesionesRestantes(int sesionesRestantes) { this.sesionesRestantes = sesionesRestantes; }

    public double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(double montoTotal) { this.montoTotal = montoTotal; }

    public double getMontoAbonado() { return montoAbonado; }
    public void setMontoAbonado(double montoAbonado) { this.montoAbonado = montoAbonado; }

    public boolean isEstadoActivo() { return estadoActivo; }
    public void setEstadoActivo(boolean estadoActivo) { this.estadoActivo = estadoActivo; }

    public String getStatusPago() { return statusPago; }
    public void setStatusPago(String statusPago) { this.statusPago = statusPago; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}