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

    // 🔥 FILTRO MAESTRO: Citas estrictamente pendientes de cobro y aprobación
    // Desaparecen automáticamente de la vista de Citas Pendientes al ser confirmadas / aprobadas
    // No incluye citas de personas ya confirmadas aunque sean del día
    @Query("SELECT c FROM Cita c WHERE (c.estado IS NULL OR UPPER(c.estado) NOT IN ('CONFIRMADO', 'CONFIRMADA', 'PAGADO', 'PAGADA', 'CANCELADO', 'CANCELADA')) " +
           "ORDER BY CASE WHEN c.fechaHora IS NULL THEN 1 ELSE 0 END, c.fechaHora ASC, c.id DESC")
    List<Cita> findCitasPendientes();

    // 🔥 CITAS AGENDADAS: Todas las citas activas con fecha/hora asignada para la sección Agenda
    @Query("SELECT c FROM Cita c WHERE c.fechaHora IS NOT NULL AND (c.estado IS NULL OR UPPER(c.estado) NOT IN ('CANCELADO', 'CANCELADA')) " +
           "ORDER BY c.fechaHora ASC, c.id DESC")
    List<Cita> findCitasAgendadas();

    @Modifying
    @Query("UPDATE Cita c SET c.cliente = null WHERE c.cliente.id = :clienteId")
    void desasociarCliente(@Param("clienteId") Long clienteId);
}
