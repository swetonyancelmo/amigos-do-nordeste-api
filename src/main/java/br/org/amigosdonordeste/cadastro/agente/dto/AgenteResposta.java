package br.org.amigosdonordeste.cadastro.agente.dto;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Nunca leva o hash do token. O codigo de convite so aparece enquanto nao foi usado. */
public record AgenteResposta(
    UUID id,
    String nome,
    boolean ativo,

    @Schema(description = "Código de seis dígitos para mandar à agente. Null depois de usado.", example = "472916")
    String codigoConvite,

    @Schema(description = "Quando o aparelho foi ativado; null se o convite ainda não foi usado")
    OffsetDateTime ativadoEm
) {
    public static AgenteResposta de(Agente a) {
        return new AgenteResposta(a.getId(), a.getNome(), a.isAtivo(), a.getCodigoConvite(), a.getAtivadoEm());
    }
}
