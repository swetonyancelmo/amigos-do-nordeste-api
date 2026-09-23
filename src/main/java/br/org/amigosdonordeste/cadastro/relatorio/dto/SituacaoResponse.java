package br.org.amigosdonordeste.cadastro.relatorio.dto;

public record SituacaoResponse(
  Indicador semBanheiro,
  Indicador soCarroPipa,
  Indicador soBolsaFamilia,
  Indicador semTratamentoAgua
) {
  public record Indicador(long valor, double percentual) {}
}
