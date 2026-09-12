package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.comunidade.enums.TipoComunidade;

import java.math.BigDecimal;
import java.util.UUID;

public record ComunidadeResponse (
      UUID id,
      String nome,
      UUID municipioId,
      String municipioNome,
      TipoComunidade tipo,
      String liderNome,
      String liderTelefone,
      BigDecimal latitude,
      BigDecimal longitude,
      String observaces
) {
      public static ComunidadeResponse fromEntity(Comunidade comunidade) {
        return new ComunidadeResponse(
          comunidade.getId(),
          comunidade.getNome(),
          comunidade.getMunicipio().getId(),
          comunidade.getMunicipio().getNome(),
          comunidade.getTipo(),
          comunidade.getLiderNome(),
          comunidade.getLiderTelefone(),
          comunidade.getLatitude(),
          comunidade.getLongitude(),
          comunidade.getObservacoes()
        );
      }

}

