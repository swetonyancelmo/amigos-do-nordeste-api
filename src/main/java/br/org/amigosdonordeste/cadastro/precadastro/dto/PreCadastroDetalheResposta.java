package br.org.amigosdonordeste.cadastro.precadastro.dto;

import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.precadastro.SituacaoPreCadastro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * GET /api/pre-cadastros/{id}: o chamado inteiro, para a tela de revisão.
 * O que a agente coletou sai do payload guardado; o resto, das relações.
 */
public record PreCadastroDetalheResposta(
    UUID id,
    SituacaoPreCadastro situacao,
    String agenteNome,
    OffsetDateTime recebidoEm,
    OffsetDateTime avaliadoEm,

    @Schema(description = "Quando a agente fez o cadastro no aparelho (pode ser dias antes do envio)")
    OffsetDateTime criadoEm,

    String motivoDevolucao,

    @Schema(description = "Família criada na aprovação; null enquanto não aprovado")
    UUID familiaId,

    String responsavelNome,
    String telefone,
    String pontoReferencia,

    @Schema(description = "Null quando o servidor não reconheceu a comunidade enviada — aí a aprovação exige comunidadeId")
    UUID comunidadeId,

    @Schema(description = "Nome da comunidade cadastrada ou, sem vínculo, o que a agente escreveu")
    String comunidadeNome,

    @Schema(description = "Família já cadastrada que pode ser a mesma, ou null. Só avisa.")
    PossivelDuplicata possivelDuplicata,

    List<PessoaColetada> pessoas
) {

    /** Uma pessoa como a agente coletou. indice é o que a aprovação usa em pessoas[].indice. */
    public record PessoaColetada(
        @Schema(description = "Posição na lista (começa em 0); é a referência da aprovação")
        int indice,
        String nome,
        boolean cadastroIncompleto,
        Sexo sexo,
        LocalDate dataNascimento,
        Integer idadeEstimada,
        LocalDate idadeEstimadaEm,
        @Schema(description = "Calculada na hora; null quando não há data nem estimativa")
        Integer idade
    ) { }
}
