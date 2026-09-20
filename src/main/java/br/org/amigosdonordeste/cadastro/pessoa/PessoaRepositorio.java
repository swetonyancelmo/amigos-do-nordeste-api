package br.org.amigosdonordeste.cadastro.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PessoaRepositorio extends JpaRepository<Pessoa, UUID> {

    List<Pessoa> findByFamiliaId(UUID familiaId);

    /** RF-09: cadastros marcados para completar depois. */
    List<Pessoa> findByCadastroIncompletoTrue();

    /**
     * Issue #18: pessoas no escopo do relatório de necessidades. comunidadeId
     * e municipioId são opcionais — sem os dois, traz todo mundo. Só filtra
     * (join, não join fetch): quem chama usa apenas os campos da própria
     * pessoa, então carregar família/comunidade aqui seria trabalho à toa.
     */
    @Query("""
        select p from Pessoa p
        join p.familia f
        join f.comunidade c
        where (:comunidadeId is null or c.id = :comunidadeId)
          and (:municipioId is null or c.municipio.id = :municipioId)
        """)
    List<Pessoa> buscarParaRelatorioNecessidades(@Param("comunidadeId") UUID comunidadeId,
                                                @Param("municipioId") UUID municipioId);
}
