package br.org.amigosdonordeste.cadastro.familia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamiliaRepositorio extends JpaRepository<Familia, UUID> {

    /**
     * Issue #17: a ficha completa que a tela de edicao carrega. Join fetch de
     * comunidade, municipio e pessoas (a colecao que mais importa nao
     * duplicar/nao fazer N+1). fontesRenda e abastecimentoAgua carregam lazy,
     * dentro da mesma transacao do service — nao dá pra fazer join fetch dos
     * dois ao mesmo tempo que pessoas: duas colecoes List no mesmo join
     * (MultipleBagFetchException) so se resolveria virando Set, e aí o join
     * das tres colecoes juntas vira produto cartesiano (linhas =
     * |pessoas| x |fontesRenda| x |abastecimentoAgua|) so pra montar uma
     * familia. Preferimos as duas consultas extras, pequenas e por familia_id.
     *
     * O "distinct" aqui nao e sobre colunas: com join fetch de pessoas, uma
     * familia com N membros vira N linhas no ResultSet. Sem distinct,
     * getSingleResult() (usado pelo Spring Data pra métodos que devolvem
     * Optional<T>) ve mais de uma linha e explode com
     * IncorrectResultSizeDataAccessException — 500 em vez de 200/404. O
     * Hibernate colapsa essas linhas de volta pra uma unica Familia antes
     * da checagem de tamanho.
     */
    @Query("""
        select distinct f from Familia f
        join fetch f.comunidade c
        join fetch c.municipio
        left join fetch f.pessoas
        where f.id = :id
        """)
    Optional<Familia> buscarDetalhePorId(@Param("id") UUID id);

    /**
     * RF-02: busca pelo nome da responsavel. join fetch de comunidade e
     * municipio porque a lista de resultados mostra os dois — sem isso, um
     * SELECT por linha (N+1).
     */
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

    /**
     * Deteccao de duplicata por nome, dentro da comunidade. unaccent (extensao
     * instalada na V1) porque o cadastro vem de papel: "Jose" tem que achar
     * "José". No perfil de teste o H2 recebe um alias UNACCENT feito em Java
     * (ver application-test.yml).
     */
    @Query(value = """
        select f.* from familia f
        where f.comunidade_id = :comunidadeId
          and unaccent(lower(trim(f.responsavel_nome))) = unaccent(lower(trim(:nome)))
        order by f.responsavel_nome
        """, nativeQuery = true)
    List<Familia> buscarPorNomeParecidoNaComunidade(@Param("comunidadeId") UUID comunidadeId,
                                                    @Param("nome") String nome);

    /**
     * Issue #18: total de famílias no escopo do relatório de necessidades.
     * comunidadeId e municipioId são opcionais — sem os dois, conta todas.
     */
    @Query("""
        select count(f) from Familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
        """)
    long contarParaRelatorioNecessidades(@Param("comunidadeId") UUID comunidadeId,
                                        @Param("municipioId") UUID municipioId);
}
