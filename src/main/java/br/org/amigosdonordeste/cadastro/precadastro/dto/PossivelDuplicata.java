package br.org.amigosdonordeste.cadastro.precadastro.dto;

import br.org.amigosdonordeste.cadastro.precadastro.MotivoDuplicata;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Familia ja cadastrada que pode ser a mesma do pre-cadastro. So avisa; nao bloqueia. */
public record PossivelDuplicata(
    @Schema(description = "Família já cadastrada que pode ser a mesma")
    UUID familiaId,

    @Schema(description = "Nome da responsável na família já cadastrada")
    String nome,

    MotivoDuplicata motivo
) { }
