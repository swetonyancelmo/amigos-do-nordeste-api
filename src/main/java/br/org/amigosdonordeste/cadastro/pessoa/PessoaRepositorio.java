package br.org.amigosdonordeste.cadastro.pessoa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaRepositorio extends JpaRepository<Pessoa, UUID>, JpaSpecificationExecutor<Pessoa> {

    /**
     * GET /api/pessoas: cada linha mostra família, comunidade e município.
     * Tudo ManyToOne, então dá para trazer junto na própria consulta paginada
     * (sem o problema de paginar join fetch de coleção) — e sem N+1. O count
     * ignora o grafo.
     */
    @Override
    @EntityGraph(attributePaths = {"familia", "familia.comunidade", "familia.comunidade.municipio"})
    Page<Pessoa> findAll(Specification<Pessoa> spec, Pageable pageable);

    /** GET /api/pessoas/{id}: a pessoa com família, comunidade e município. */
    @Query("""
        select p from Pessoa p
        join fetch p.familia f
        join fetch f.comunidade c
        join fetch c.municipio
        where p.id = :id
        """)
    Optional<Pessoa> buscarDetalhePorId(@Param("id") UUID id);

    List<Pessoa> findByFamiliaId(UUID familiaId);

    /** RF-09: cadastros marcados para completar depois. */
    List<Pessoa> findByCadastroIncompletoTrue();

    /**
     * Issue #18: pessoas no escopo do relatório de necessidades. comunidadeId
     * e municipioId são opcionais — sem os dois, traz todo mundo. Só filtra
     * (join, não join fetch): quem chama usa apenas os campos da própria
     * pessoa, então carregar família/comunidade aqui seria trabalho à toa.
     * Pessoa de família inativa não entra (issue #43).
     */
    @Query("""
        select p from Pessoa p
        join p.familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
          and f.ativa = true
        """)
    List<Pessoa> buscarParaRelatorioNecessidades(@Param("comunidadeId") UUID comunidadeId,
                                                @Param("municipioId") UUID municipioId);
}
