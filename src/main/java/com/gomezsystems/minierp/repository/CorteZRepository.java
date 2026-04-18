package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.CorteZ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorteZRepository extends JpaRepository<CorteZ, Long> {
    List<CorteZ> findAllByOrderByFechaGeneracionDesc();
}