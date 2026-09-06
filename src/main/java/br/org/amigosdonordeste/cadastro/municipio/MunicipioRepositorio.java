package br.org.amigosdonordeste.cadastro.municipio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MunicipioRepositorio extends JpaRepository<Municipio, UUID> {

    Optional<Municipio> findByCodigoIbge(String codigoIbge);

    List<Municipio> findAllByOrderByNomeAsc();
}
