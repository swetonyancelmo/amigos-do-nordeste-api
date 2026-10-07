package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.relatorio.PontoComunidadeResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComunidadeRepositorio extends JpaRepository<Comunidade, UUID> {

  List<Comunidade> findByMunicipioIdOrderByNomeAsc(UUID municipioId);

  @Query("SELECT c FROM Comunidade c JOIN FETCH c.municipio ORDER BY c.nome ASC")
  List<Comunidade> listarComMunicipio();

  @Query("""
    SELECT new br.org.amigosdonordeste.cadastro.relatorio.PontoComunidadeResponse(
        c.id, c.nome, c.latitude, c.longitude, COUNT(f.id))
    FROM Comunidade c
    LEFT JOIN Familia f ON f.comunidade.id = c.id
    GROUP BY c.id, c.nome, c.latitude, c.longitude
    """)
  List<PontoComunidadeResponse> buscarPontosAgregados();

  @Query("""
    SELECT new br.org.amigosdonordeste.cadastro.relatorio.PontoComunidadeResponse(
        c.id, c.nome, c.latitude, c.longitude, COUNT(f.id))
    FROM Comunidade c
    LEFT JOIN Familia f ON f.comunidade.id = c.id
    WHERE c.municipio.id = :municipioId
    GROUP BY c.id, c.nome, c.latitude, c.longitude
    """)
  List<PontoComunidadeResponse> buscarPontosAgregadosPorMunicipio(@Param("municipioId") UUID municipioId);
}
