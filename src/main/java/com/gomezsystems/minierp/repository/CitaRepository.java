package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 🔥 Importamos las librerías para manejar fechas y listas
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // 🔥 LA MAGIA: Spring Boot leerá este nombre y creará la consulta SQL automáticamente
    List<Cita> findByEstadoAndFechaCreacionBefore(String estado, LocalDateTime limite);

}