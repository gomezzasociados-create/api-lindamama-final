package com.gomezsystems.minierp.repository;

import com.gomezsystems.minierp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByPinAndActivoTrue(String pin);
    Optional<Usuario> findByUsername(String username);
    List<Usuario> findAllByOrderByNombreAsc();
}
