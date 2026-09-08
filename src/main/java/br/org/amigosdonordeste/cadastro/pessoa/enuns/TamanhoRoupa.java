package br.org.amigosdonordeste.cadastro.pessoa.enuns;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;

/**
 * Tamanho de roupa do membro. Alimenta a contagem de roupa por comunidade
 * (RF-03) — e contagem, nunca coluna de total.
 *
 * Lista fechada, valores provisorios — a associacao usa faixas propria nos
 * documentos atuais; confirmar e servir por /api/metadados.
 */
public enum TamanhoRoupa implements Rotulavel {

    PP("PP"), P("P"), M("M"), G("G"), GG("GG");

    private final String rotulo;
    TamanhoRoupa(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
