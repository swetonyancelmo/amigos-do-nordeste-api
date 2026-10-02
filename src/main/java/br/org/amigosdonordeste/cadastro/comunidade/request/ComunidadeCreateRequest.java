package br.org.amigosdonordeste.cadastro.comunidade.request;

import br.org.amigosdonordeste.cadastro.comunidade.enums.TipoComunidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ComunidadeCreateRequest(
  @NotBlank
  @Size(max = 120)
  String nome,

  @NotNull
  UUID municipioId,

  TipoComunidade tipoComunidade,

  @Size(max = 120)
  String liderNome,

  // mesmo tamanho da coluna (V7): passar daqui virava o 400 genérico do banco
  @Size(max = 20)
  String liderTelefone,

  BigDecimal latitude,
  BigDecimal longitude,

  String observacoes
) {
}
