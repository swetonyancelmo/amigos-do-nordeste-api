package br.org.amigosdonordeste.cadastro.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmailIgnoreCaseAndAtivoTrue(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
}
