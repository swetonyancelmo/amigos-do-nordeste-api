package br.org.amigosdonordeste.cadastro.pessoa.enuns;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;
/**
 * Parentesco do membro com a pessoa responsavel pela familia. Lista fechada,
 * valores provisorios — confirmar com a associacao e servir por /api/metadados.
 */
public enum Parentesco implements Rotulavel {

    RESPONSAVEL("Responsável"),
    CONJUGE("Cônjuge"),
    FILHO("Filho"),
    ENTEADO("Enteado"),
    PAI_OU_MAE("Pai ou mãe"),
    SOGRO("Sogro"),
    GENRO_OU_NORA("Genro ou nora"),
    NETO("Neto"),
    AVO("Avô"),
    IRMAO("Irmão"),
    OUTRO_PARENTE("Outro parente"),
    AGREGADO("Agregado"),
    SEM_PARENTESCO("Sem parentesco");

    private final String rotulo;

    Parentesco(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String getRotulo() {
        return rotulo;
    }
}
