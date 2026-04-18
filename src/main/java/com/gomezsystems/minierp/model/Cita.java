package com.gomezsystems.minierp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tu relación con el CRM se queda intacta
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Campos para el catálogo y la agenda
    private String nombreTratamiento; // Aquí guardaremos el resumen de lo que compró
    private LocalDateTime fechaHora;
    private String estado; // PENDIENTE, CONFIRMADA, RESERVADO, etc.

    @ManyToOne
    @JoinColumn(name = "socia_id")
    private Socia socia; // La masajista elegida

    // CAMPOS ADICIONALES PARA EL POS
    private Integer totalPagado; 
    private Double montoTotal;   // Precio total del servicio
    private Double montoPagado;  // Suma de lo abonado hasta ahora
    private String metodoPago; 

    private String numeroOrden;

    // 🔥 NUEVO: El cronómetro para saber cuándo caduca la reserva (10 min) 🔥
    private LocalDateTime fechaCreacion;

    public Cita() {}

    // --- GETTERS Y SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public String getNombreTratamiento() { return nombreTratamiento; }
    public void setNombreTratamiento(String nombreTratamiento) { this.nombreTratamiento = nombreTratamiento; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getTotalPagado() { return totalPagado; }
    public void setTotalPagado(Integer totalPagado) { this.totalPagado = totalPagado; }

    public Double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(Double montoTotal) { this.montoTotal = montoTotal; }

    public Double getMontoPagado() { return montoPagado; }
    public void setMontoPagado(Double montoPagado) { this.montoPagado = montoPagado; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getNumeroOrden() { return numeroOrden; }
    public void setNumeroOrden(String numeroOrden) { this.numeroOrden = numeroOrden; }

    public Socia getSocia() { return socia; }
    public void setSocia(Socia socia) { this.socia = socia; }

    // 🔥 GETTER Y SETTER DEL NUEVO CAMPO 🔥
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}