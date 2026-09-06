package br.org.amigosdonordeste.cadastro.comunidade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComunidadeRepositorio extends JpaRepository<Comunidade, UUID> {

    /** join fetch do municipio: a listagem sempre mostra o nome do municipio (evita N+1). */
    @Query("select c from Comunidade c join fetch c.municipio order by c.nome")
    List<Comunidade> listarComMunicipio();

    List<Comunidade> findByMunicipioIdOrderByNomeAsc(UUID municipioId);
}
