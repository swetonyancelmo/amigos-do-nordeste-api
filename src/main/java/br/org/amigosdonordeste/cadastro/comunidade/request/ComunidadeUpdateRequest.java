package br.org.amigosdonordeste.cadastro.comunidade.request;

import br.org.amigosdonordeste.cadastro.comunidade.enums.TipoComunidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ComunidadeUpdateRequest(

  @NotBlank
  @Size(max = 120)
  String nome,

  @NotNull
  UUID municipioId,

  TipoComunidade tipoComunidade,

  @Size(max = 120)
  String liderNome,

  @Size(max = 20)
  String liderTelefone,

  BigDecimal latitude,
  BigDecimal longitude,

  String observacoes
) {
}
