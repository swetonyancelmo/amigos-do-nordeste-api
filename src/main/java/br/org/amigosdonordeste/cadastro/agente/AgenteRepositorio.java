package br.org.amigosdonordeste.cadastro.agente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgenteRepositorio extends JpaRepository<Agente, UUID> {
    Optional<Agente> findByTokenHashAndAtivoTrue(String tokenHash);
}
