package br.org.amigosdonordeste.cadastro.precadastro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Corpo do POST /api/pre-cadastros/{id}/devolver. O motivo e obrigatorio: a agente precisa saber o que corrigir. */
public record DevolverPreCadastroRequisicao(
    @Schema(example = "Faltou a idade das duas crianças.")
    @NotBlank String motivo
) { }
