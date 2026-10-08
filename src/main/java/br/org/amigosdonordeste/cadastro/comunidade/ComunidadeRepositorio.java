package br.org.amigosdonordeste.cadastro.comunidade;

import org.springframework.data.jpa.repository.JpaRepository;
import br.org.amigosdonordeste.cadastro.relatorio.PontoComunidadeResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ComunidadeRepositorio extends JpaRepository<Comunidade, UUID> {

    /** join fetch do municipio: a listagem sempre mostra o nome do municipio (evita N+1). */
    @Query("select c from Comunidade c join fetch c.municipio order by c.nome")
    List<Comunidade> listarComMunicipio();

    List<Comunidade> findByMunicipioIdOrderByNomeAsc(UUID municipioId);

    /**
     * Mapa (ADR-0005): uma linha por comunidade, numa consulta só. Família
     * inativa não conta (issue #43); comunidade sem família volta com 0.
     * municipioId nulo = todos os municípios.
     */
    @Query("""
            select new br.org.amigosdonordeste.cadastro.relatorio.PontoComunidadeResponse(
                c.id, c.nome, c.latitude, c.longitude, count(f.id))
            from Comunidade c
            left join Familia f on f.comunidade = c and f.ativa = true
            where :municipioId is null or c.municipio.id = :municipioId
            group by c.id, c.nome, c.latitude, c.longitude
            order by c.nome
            """)
    List<PontoComunidadeResponse> contarFamiliasPorComunidade(@Param("municipioId") UUID municipioId);
}
