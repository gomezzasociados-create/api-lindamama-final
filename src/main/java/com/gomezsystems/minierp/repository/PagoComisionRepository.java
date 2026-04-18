package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.PagoComision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoComisionRepository extends JpaRepository<PagoComision, Long> {
    List<PagoComision> findBySociaIdOrderByFechaHoraDesc(Long sociaId);
}
