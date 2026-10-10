package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

/** Comparação de uma faixa: "razão MAIOR que 1", "IGUAL a 1", "MENOR que 1" (Tabela 01 do artigo). */
public enum OperadorFaixa {
    MENOR,
    IGUAL,
    MAIOR;

    boolean aceita(int comparacao) {
        return switch (this) {
            case MENOR -> comparacao < 0;
            case IGUAL -> comparacao == 0;
            case MAIOR -> comparacao > 0;
        };
    }
}
