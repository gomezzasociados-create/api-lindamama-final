package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 🔥 Importamos las librerías para manejar fechas y listas
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // 🔥 LA MAGIA: Spring Boot leerá este nombre y creará la consulta SQL automáticamente
    List<Cita> findByEstadoAndFechaCreacionBefore(String estado, LocalDateTime limite);

    List<Cita> findBySociaIdAndFechaHoraBetweenOrderByFechaHoraAsc(Long sociaId, LocalDateTime inicio, LocalDateTime fin);

    List<Cita> findBySociaIdAndFechaHoraAfterOrderByFechaHoraAsc(Long sociaId, LocalDateTime inicio);

    @Modifying
    @Query("UPDATE Cita c SET c.cliente = null WHERE c.cliente.id = :clienteId")
    void desasociarCliente(@Param("clienteId") Long clienteId);
}