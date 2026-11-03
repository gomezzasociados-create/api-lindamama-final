package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.ConfiguracionSeguridad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionSeguridadRepository extends JpaRepository<ConfiguracionSeguridad, Long> {
    Optional<ConfiguracionSeguridad> findByClave(String clave);
}
