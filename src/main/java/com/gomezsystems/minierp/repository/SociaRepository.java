package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Socia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SociaRepository extends JpaRepository<Socia, Long> {
    List<Socia> findByActivaTrue();
    Optional<Socia> findByUsuario(String usuario);
}
