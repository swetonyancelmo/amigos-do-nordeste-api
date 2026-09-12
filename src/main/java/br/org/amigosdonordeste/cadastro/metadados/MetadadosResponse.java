package br.org.amigosdonordeste.cadastro.metadados;

import java.util.List;

public record MetadadosResponse(
    List<OpcaoDTO> tipoComunidade,
    List<OpcaoDTO> sexo,
    List<OpcaoDTO> abastecimentoAgua,
    List<OpcaoDTO> escoamentoSanitario,
    List<OpcaoDTO> tratamentoAgua,
    List<OpcaoDTO> tipoFonteRenda,
    List<OpcaoDTO> faixaRenda,
    List<OpcaoDTO> serie,
    List<OpcaoDTO> tamanhoRoupa,
    List<OpcaoDTO> numeroCalcado
) {
}