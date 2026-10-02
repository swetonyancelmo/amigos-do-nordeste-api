package br.org.amigosdonordeste.cadastro.relatorio;

/**
 * Issue #19: o recorte que a associação leva para prefeitura, edital e
 * doador. Cada indicador é contado na hora (regra 2 do projeto) e o
 * percentual é sobre o total de famílias do filtro.
 */
public record SituacaoResponse(
        long totalFamilias,
        Indicador semBanheiro,
        Indicador soCarroPipa,
        Indicador soBolsaFamilia,
        Indicador semTratamentoAgua
) {

    /** percentual de 0 a 100, com duas casas; 0 quando o filtro não tem família. */
    public record Indicador(long valor, double percentual) {

        static Indicador de(long valor, long total) {
            if (total == 0) {
                return new Indicador(0, 0.0);
            }
            return new Indicador(valor, Math.round(valor * 10000.0 / total) / 100.0);
        }
    }
}
