package br.org.amigosdonordeste.cadastro.fonterenda.enums;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;
/**
 * Faixa de renda em salarios minimos, NUNCA valor em reais: renda declarada por
 * um vizinho, no papel, sobre trabalho sazonal, e o dado menos confiavel do
 * cadastro — e o mais sensivel (ver docs/decisoes/ADR-0003).
 *
 * Lista fechada, valores provisorios — a servir por /api/metadados.
 */
public enum FaixaRenda implements Rotulavel {

    SEM_RENDA_FIXA("Sem renda fixa"),
    ATE_1_SALARIO("Até 1 salário mínimo"),
    DE_1_A_2_SALARIOS("De 1 a 2 salários mínimos"),
    MAIS_DE_2_SALARIOS("Mais de 2 salários mínimos");

    private final String rotulo;
    FaixaRenda(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}