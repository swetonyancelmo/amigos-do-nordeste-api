package br.org.amigosdonordeste.cadastro.municipio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MunicipioRepositorio extends JpaRepository<Municipio, UUID> {

    /** Lista em ordem alfabética — o critério de aceite pede ordenado por nome. */
    List<Municipio> findAllByOrderByNomeAsc();

    /** Para o POST: já existe alguém com esse código? */
    boolean existsByCodigoIbge(String codigoIbge);

    /** Para o PUT: existe OUTRO município com esse código? (ignorando ele mesmo) */
    boolean existsByCodigoIbgeAndIdNot(String codigoIbge, UUID id);
}
