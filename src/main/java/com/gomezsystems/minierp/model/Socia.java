package com.gomezsystems.minierp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "socias")
public class Socia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String especialidad;
    private String descripcion;
    private String telefono;
    private String usuario;
    private String password;


    
    @Column(columnDefinition = "TEXT")
    private String fotoUrl;

    // Comisiones
    private Double porcentajeSpa;
    private Double porcentajeSocia;
    
    // Si la masajista está de turno o activa temporalmente
    private boolean activa = true;

    // Cupo (1 a 10)
    private Integer slotVirtual;

    public Socia() {}

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

    public String getEspecialidad() {
        return especialidad;
    }
    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getUsuario() {
        return usuario;
    }
    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }
    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Double getPorcentajeSpa() {
        return porcentajeSpa;
    }
    public void setPorcentajeSpa(Double porcentajeSpa) {
        this.porcentajeSpa = porcentajeSpa;
    }

    public Double getPorcentajeSocia() {
        return porcentajeSocia;
    }
    public void setPorcentajeSocia(Double porcentajeSocia) {
        this.porcentajeSocia = porcentajeSocia;
    }

    public boolean isActiva() {
        return activa;
    }
    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public Integer getSlotVirtual() {
        return slotVirtual;
    }
    public void setSlotVirtual(Integer slotVirtual) {
        this.slotVirtual = slotVirtual;
    }
}
