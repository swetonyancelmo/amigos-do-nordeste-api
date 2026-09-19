package br.org.amigosdonordeste.cadastro.familia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamiliaRepositorio extends JpaRepository<Familia, UUID> {

    /**
     * Issue #17: a ficha completa que a tela de edicao carrega. Uma consulta
     * so, com join fetch de comunidade, municipio, membros, fontes de renda e
     * abastecimento — sem isso seria um SELECT por colecao a cada abertura da
     * ficha (N+1).
     */
    @Query("""
        select f from Familia f
        join fetch f.comunidade c
        join fetch c.municipio
        left join fetch f.pessoas
        left join fetch f.fontesRenda
        left join fetch f.abastecimentoAgua
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
}
