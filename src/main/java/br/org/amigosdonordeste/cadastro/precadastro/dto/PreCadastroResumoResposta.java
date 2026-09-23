package br.org.amigosdonordeste.cadastro.precadastro.dto;

import br.org.amigosdonordeste.cadastro.precadastro.SituacaoPreCadastro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Uma linha da tela de Chamados (GET /api/pre-cadastros). */
public record PreCadastroResumoResposta(
    UUID id,

    @Schema(description = "Nome da responsável como a agente digitou")
    String responsavelNome,

    @Schema(description = "Nula quando o servidor não reconheceu a comunidade que o aparelho mandou")
    UUID comunidadeId,

    @Schema(description = "Nome da comunidade cadastrada ou, sem vínculo, o nome que a agente escreveu")
    String comunidadeNome,

    @Schema(description = "Calculado a partir das pessoas do envio, nunca lido de coluna")
    int totalPessoas,

    @Schema(description = "Nome da agente que enviou")
    String agenteNome,

    OffsetDateTime recebidoEm,

    SituacaoPreCadastro situacao,

    @Schema(description = "Família já cadastrada que pode ser a mesma, ou null. Só avisa; nada é bloqueado.")
    PossivelDuplicata possivelDuplicata
) { }
