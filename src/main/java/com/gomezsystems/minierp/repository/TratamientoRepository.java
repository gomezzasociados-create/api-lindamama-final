package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Tratamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Este es el motor que conectará tus tratamientos con la base de datos PostgreSQL
@Repository
public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {
    // Aquí puedes agregar búsquedas personalizadas en el futuro
}