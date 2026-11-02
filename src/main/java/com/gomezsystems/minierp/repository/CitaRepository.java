package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // 🔥 Citas vencidas no pagadas para limpieza automática
    List<Cita> findByEstadoAndFechaCreacionBefore(String estado, LocalDateTime limite);

    List<Cita> findBySociaIdAndFechaHoraBetweenOrderByFechaHoraAsc(Long sociaId, LocalDateTime inicio, LocalDateTime fin);

    List<Cita> findBySociaIdAndFechaHoraAfterOrderByFechaHoraAsc(Long sociaId, LocalDateTime inicio);

    // 🔥 FILTRO MAESTRO: Citas pendientes de cobro O agendadas para el día de hoy
    @Query("SELECT c FROM Cita c WHERE (c.estado IS NULL OR UPPER(c.estado) != 'CANCELADO') " +
           "AND (UPPER(c.estado) NOT IN ('CONFIRMADO', 'PAGADO') OR (c.fechaHora >= :inicioDia AND c.fechaHora <= :finDia)) " +
           "ORDER BY CASE WHEN (c.estado IS NULL OR UPPER(c.estado) NOT IN ('CONFIRMADO', 'PAGADO')) THEN 0 ELSE 1 END, " +
           "CASE WHEN c.fechaHora IS NULL THEN 0 ELSE 1 END, c.fechaHora ASC, c.id DESC")
    List<Cita> findCitasPendientesOAgendadasHoy(@Param("inicioDia") LocalDateTime inicioDia, @Param("finDia") LocalDateTime finDia);

    @Modifying
    @Query("UPDATE Cita c SET c.cliente = null WHERE c.cliente.id = :clienteId")
    void desasociarCliente(@Param("clienteId") Long clienteId);
}
