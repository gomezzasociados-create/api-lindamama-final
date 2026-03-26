package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // 🔥 El buscador del POS que armamos 🔥
    List<Cliente> findByTelefonoContainingOrNombreContainingIgnoreCase(String telefono, String nombre);
}