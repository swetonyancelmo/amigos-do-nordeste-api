package br.org.amigosdonordeste.cadastro.familia.repository;

import br.org.amigosdonordeste.cadastro.familia.Familia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamiliaRepositorio extends JpaRepository<Familia, UUID>, JpaSpecificationExecutor<Familia> {

  @Query("""
        select distinct f from Familia f
        join fetch f.comunidade c
        join fetch c.municipio
        left join fetch f.pessoas
        where f.id = :id
        """)
  Optional<Familia> buscarDetalhePorId(@Param("id") UUID id);

  @Query("""
        select f from Familia f
        join fetch f.comunidade c
        join fetch c.municipio
        where upper(f.responsavelNome) like upper(concat('%', :nome, '%'))
        order by f.responsavelNome
        """)
  List<Familia> buscarPorResponsavel(@Param("nome") String nome);

  @Query("""
        select f from Familia f
        join fetch f.comunidade c
        join fetch c.municipio
        order by f.responsavelNome
        """)
  List<Familia> listarComComunidade();

  List<Familia> findByComunidadeIdOrderByResponsavelNomeAsc(UUID comunidadeId);

  @Query(value = """
        select f.* from familia f
        where f.comunidade_id = :comunidadeId
          and unaccent(lower(trim(f.responsavel_nome))) = unaccent(lower(trim(:nome)))
        order by f.responsavel_nome
        """, nativeQuery = true)
  List<Familia> buscarPorNomeParecidoNaComunidade(@Param("comunidadeId") UUID comunidadeId,
                                                  @Param("nome") String nome);

  @Query("""
        select count(f) from Familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
        """)
  long contarParaRelatorioNecessidades(@Param("comunidadeId") UUID comunidadeId,
                                       @Param("municipioId") UUID municipioId);

  /**
   * Issue #02: Carrega os dados da linha da listagem (comunidade, município e pessoas)
   * para os IDs já paginados, sem N+1 e sem multiplicar registros.
   */
  @Query("""
        select distinct f from Familia f
        join fetch f.comunidade c
        join fetch c.municipio
        left join fetch f.pessoas
        where f.id in :ids
        order by f.responsavelNome asc
    """)
  List<Familia> buscarComPessoasPorIds(@Param("ids") List<UUID> ids);
}
