package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Egreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EgresoRepository extends JpaRepository<Egreso, Long> {
    // Esta línea le permite al sistema buscar los gastos de hoy que aún no se han cerrado
    List<Egreso> findByCierreAplicadoFalseOrderByFechaHoraDesc();
    
    // Método agregado para el historial en Tesorería
    List<Egreso> findAllByOrderByFechaHoraDesc();
}