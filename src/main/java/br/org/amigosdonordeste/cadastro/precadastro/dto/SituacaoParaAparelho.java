package br.org.amigosdonordeste.cadastro.precadastro.dto;

import br.org.amigosdonordeste.cadastro.precadastro.SituacaoPreCadastro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * O que o aparelho da agente recebe sobre o que ela mesma enviou: só a
 * situação e, quando devolvido, o motivo. Nenhum dado de família (ADR-0002).
 */
public record SituacaoParaAparelho(
    UUID id,
    SituacaoPreCadastro situacao,
    @Schema(description = "Só quando DEVOLVIDO: o que a associação pediu para corrigir")
    String motivoDevolucao
) { }
