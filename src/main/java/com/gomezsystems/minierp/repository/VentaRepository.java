package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // Esta línea es VITAL. Es la que usa tu controlador para buscar
    // las ventas de hoy que aún no han sido cerradas en el Reporte Z.
    List<Venta> findByCierreAplicadoFalseOrderByFechaHoraDesc();

    List<Venta> findByClienteIdOrderByFechaHoraDesc(Long clienteId);

    List<Venta> findBySociaIdAndLiquidadoSociaFalseOrderByFechaHoraDesc(Long sociaId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(v.montoSocia) FROM Venta v WHERE v.socia.id = :sociaId AND v.liquidadoSocia = false")
    Double sumMontoSociaBySociaIdAndLiquidadoSociaFalse(@Param("sociaId") Long sociaId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(v.montoSocia) FROM Venta v WHERE v.socia.id = :sociaId AND v.fechaHora BETWEEN :inicio AND :fin")
    Double sumMontoSociaBySociaIdAndFechaHoraBetween(@Param("sociaId") Long sociaId, @Param("inicio") java.time.LocalDateTime inicio, @Param("fin") java.time.LocalDateTime fin);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(v.totalPagado) FROM Venta v WHERE v.estado = 'PAGADO'")
    Double sumTotalPagadoByEstadoPagado();

    List<Venta> findByEstadoAndFechaHoraBefore(String estado, java.time.LocalDateTime limite);
}