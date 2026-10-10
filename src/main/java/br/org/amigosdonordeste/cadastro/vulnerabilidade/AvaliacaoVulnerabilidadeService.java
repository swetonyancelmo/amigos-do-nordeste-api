package br.org.amigosdonordeste.cadastro.vulnerabilidade;

import br.org.amigosdonordeste.cadastro.dominio.Idade;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.base.BaseDeConhecimentoService;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Avaliacao;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.FatosFamilia;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.MotorDeInferencia;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Liga o cadastro ao motor de inferência (ADR-0010): carrega a base de
 * conhecimento, traduz a entidade em FatosFamilia e avalia. Não grava nada:
 * o escore é calculado a cada leitura, como todo total deste projeto.
 *
 * Também não decide nada. Nenhuma ação do sistema parte da avaliação: ela só
 * é mostrada, e quem prioriza é a pessoa.
 */
@Service
public class AvaliacaoVulnerabilidadeService {

    private final BaseDeConhecimentoService baseDeConhecimento;

    public AvaliacaoVulnerabilidadeService(BaseDeConhecimentoService baseDeConhecimento) {
        this.baseDeConhecimento = baseDeConhecimento;
    }

    /** Para avaliar muitas famílias: carregue a base uma vez e chame {@link #avaliar(Familia, BaseDeConhecimento, LocalDate)}. */
    public BaseDeConhecimento carregarBase() {
        return baseDeConhecimento.carregar();
    }

    public Avaliacao avaliar(Familia familia) {
        return avaliar(familia, carregarBase(), LocalDate.now());
    }

    /** Precisa das pessoas, fontes de renda e abastecimento carregados (dentro da transação). */
    public static Avaliacao avaliar(Familia familia, BaseDeConhecimento base, LocalDate hoje) {
        return MotorDeInferencia.avaliar(fatos(familia, hoje), base, hoje);
    }

    static FatosFamilia fatos(Familia familia, LocalDate hoje) {
        return new FatosFamilia(
                familia.getNumeroComodos(),
                familia.getTemBanheiro(),
                familia.getEscoamentoSanitario(),
                familia.getTratamentoAgua(),
                familia.getAbastecimentoAgua(),
                familia.getFaixaRenda(),
                familia.getFontesRenda().stream().map(FonteRenda::getTipo).toList(),
                familia.getPessoas().stream()
                        .map(p -> new FatosFamilia.Pessoa(
                                p.getDataNascimento(),
                                Idade.calcular(p.getDataNascimento(), p.getIdadeEstimada(), p.getIdadeEstimadaEm(), hoje)))
                        .toList());
    }
}
