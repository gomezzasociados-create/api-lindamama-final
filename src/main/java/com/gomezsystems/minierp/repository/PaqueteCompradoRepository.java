package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.PaqueteComprado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaqueteCompradoRepository extends JpaRepository<PaqueteComprado, Long> {
    // Si necesitamos buscar todos los paquetes de una clienta específica:
    List<PaqueteComprado> findByClienteId(Long clienteId);
}