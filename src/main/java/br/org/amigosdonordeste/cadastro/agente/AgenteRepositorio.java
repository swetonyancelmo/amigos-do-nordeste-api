package br.org.amigosdonordeste.cadastro.agente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AgenteRepositorio extends JpaRepository<Agente, UUID> {
    Optional<Agente> findByTokenHashAndAtivoTrue(String tokenHash);

    /**
     * Consome o codigo de convite em um UPDATE condicional: so quem ainda
     * encontra o codigo na linha grava o hash. Dois pedidos simultaneos com o
     * mesmo codigo disputam a mesma linha no banco e apenas um devolve 1 —
     * o outro ve 0 e recebe o mesmo erro de codigo invalido.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Agente a
           SET a.tokenHash = :tokenHash, a.codigoConvite = null, a.ativadoEm = :agora
         WHERE a.codigoConvite = :codigo AND a.ativo = true
        """)
    int consumirCodigoConvite(@Param("codigo") String codigo,
                              @Param("tokenHash") String tokenHash,
                              @Param("agora") OffsetDateTime agora);
}
