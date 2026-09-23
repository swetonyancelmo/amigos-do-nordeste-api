package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
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
          and f.ativa = true
        order by f.responsavelNome
        """)
    List<Familia> buscarPorResponsavel(@Param("nome") String nome);

    /**
     * Issue #43: GET /api/familias. Inativa fica de fora, a nao ser que
     * incluirInativas venha true — e o unico jeito de achar uma para reativar.
     * join fetch pelo mesmo motivo da busca acima (N+1).
     */
    @Query("""
        select f from Familia f
        join fetch f.comunidade c
        join fetch c.municipio
        where (:incluirInativas = true or f.ativa = true)
        order by f.responsavelNome
        """)
    List<Familia> listar(@Param("incluirInativas") boolean incluirInativas);

    /** Duplicata do pre-cadastro: so contra familia ativa (ver buscarPorNomeParecidoNaComunidade). */
    List<Familia> findByComunidadeIdAndAtivaTrueOrderByResponsavelNomeAsc(UUID comunidadeId);

    /**
     * Deteccao de duplicata por nome, dentro da comunidade. unaccent (extensao
     * instalada na V1) porque o cadastro vem de papel: "Jose" tem que achar
     * "José". No perfil de teste o H2 recebe um alias UNACCENT feito em Java
     * (ver application-test.yml).
     *
     * Familia inativa nao conta: em geral foi inativada justamente por ser
     * duplicata ou erro, e apontar para ela mandaria a revisora para um
     * registro que ja saiu da base.
     */
    @Query(value = """
        select f.* from familia f
        where f.comunidade_id = :comunidadeId
          and f.ativa = true
          and unaccent(lower(trim(f.responsavel_nome))) = unaccent(lower(trim(:nome)))
        order by f.responsavel_nome
        """, nativeQuery = true)
    List<Familia> buscarPorNomeParecidoNaComunidade(@Param("comunidadeId") UUID comunidadeId,
                                                    @Param("nome") String nome);

    /**
     * Issue #18: total de famílias no escopo do relatório de necessidades.
     * Também é o denominador dos percentuais da issue #19 (situação).
     * comunidadeId e municipioId são opcionais — sem os dois, conta todas.
     * Toda contagem daqui para baixo ignora família inativa (issue #43).
     */
    @Query("""
        select count(f) from Familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
          and f.ativa = true
        """)
    long contarParaRelatorioNecessidades(@Param("comunidadeId") UUID comunidadeId,
                                        @Param("municipioId") UUID municipioId);

    /*
     * Issue #19: indicadores de situacao. Cada um e um count no banco, com o
     * mesmo filtro opcional de comunidade/municipio do relatorio de
     * necessidades — carregar as familias e percorrer abastecimentoAgua e
     * fontesRenda em Java faria duas consultas extras por familia (N+1).
     * O total de familias do escopo vem de contarParaRelatorioNecessidades.
     */

    /** tem_banheiro nao e true: false ou nao informado. */
    @Query("""
        select count(f) from Familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
          and f.ativa = true
          and (f.temBanheiro is null or f.temBanheiro = false)
        """)
    long contarSemBanheiro(@Param("comunidadeId") UUID comunidadeId,
                           @Param("municipioId") UUID municipioId);

    @Query("""
        select count(f) from Familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
          and f.ativa = true
          and f.tratamentoAgua = :tratamento
        """)
    long contarPorTratamentoAgua(@Param("comunidadeId") UUID comunidadeId,
                                 @Param("municipioId") UUID municipioId,
                                 @Param("tratamento") TratamentoAgua tratamento);

    /** Abastecimento com exatamente um item, e esse item e o informado. */
    @Query("""
        select count(f) from Familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
          and f.ativa = true
          and size(f.abastecimentoAgua) = 1
          and :abastecimento member of f.abastecimentoAgua
        """)
    long contarSoComAbastecimento(@Param("comunidadeId") UUID comunidadeId,
                                  @Param("municipioId") UUID municipioId,
                                  @Param("abastecimento") AbastecimentoAgua abastecimento);

    /** Fontes de renda com exatamente um item, e esse item e do tipo informado. */
    @Query("""
        select count(f) from Familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
          and f.ativa = true
          and size(f.fontesRenda) = 1
          and exists (select 1 from FonteRenda fr where fr.familia = f and fr.tipo = :tipo)
        """)
    long contarSoComFonteRenda(@Param("comunidadeId") UUID comunidadeId,
                               @Param("municipioId") UUID municipioId,
                               @Param("tipo") TipoFonteRenda tipo);
}
