package br.org.amigosdonordeste.cadastro.precadastro;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreCadastroRepositorio extends JpaRepository<PreCadastro, UUID> {

    /**
     * Fila de revisao, mais antigo primeiro. join fetch de agente e comunidade
     * porque a lista mostra os dois — sem isso, dois SELECTs por linha.
     */
    @Query("""
        select p from PreCadastro p
        join fetch p.agente
        left join fetch p.comunidade
        where p.situacao = :situacao
        order by p.recebidoEm
        """)
    List<PreCadastro> listarPorSituacao(@Param("situacao") SituacaoPreCadastro situacao);

    @Query("""
        select p from PreCadastro p
        join fetch p.agente
        left join fetch p.comunidade
        order by p.recebidoEm
        """)
    List<PreCadastro> listarTodos();

    /**
     * Para aprovar/devolver: tranca a linha ate o fim da transacao. Dois
     * cliques em "aprovar" ao mesmo tempo passariam os dois pela checagem de
     * PENDENTE e criariam duas familias; com o lock o segundo espera o
     * primeiro e ve APROVADO.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PreCadastro p where p.id = :id")
    Optional<PreCadastro> buscarParaAvaliar(@Param("id") UUID id);
}
