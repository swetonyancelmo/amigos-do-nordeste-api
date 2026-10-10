package br.org.amigosdonordeste.cadastro.metadados;

import br.org.amigosdonordeste.cadastro.comunidade.enums.TipoComunidade;
import br.org.amigosdonordeste.cadastro.dominio.NumerosCalcado;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import br.org.amigosdonordeste.cadastro.precadastro.SituacaoPreCadastro;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.base.RegraEstratoRepositorio;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class MetadadosService {

    private final RegraEstratoRepositorio estratosVulnerabilidade;

    public MetadadosService(RegraEstratoRepositorio estratosVulnerabilidade) {
        this.estratosVulnerabilidade = estratosVulnerabilidade;
    }

    public MetadadosResponse listar() {
        return new MetadadosResponse(
            opcoes(TipoComunidade.class),
            opcoes(Sexo.class),
            opcoes(Parentesco.class),
            opcoes(AbastecimentoAgua.class),
            opcoes(EscoamentoSanitario.class),
            opcoes(TratamentoAgua.class),
            opcoes(TipoFonteRenda.class),
            opcoes(FaixaRenda.class),
            opcoes(Serie.class),
            opcoes(TamanhoRoupa.class),
            NumerosCalcado.VALORES.stream()
                .map(numero -> new OpcaoDTO(numero, numero))
                .toList(),
            opcoes(SituacaoPreCadastro.class),
            // o rótulo do estrato é configurável (ADR-0010): vem do banco, não do enum
            estratosVulnerabilidade.findAllByOrderByOrdemAsc().stream()
                .map(e -> new OpcaoDTO(e.getCodigo(), e.getRotulo()))
                .toList()
        );
    }

    private static List<OpcaoDTO> opcoes(Class<? extends Rotulavel> tipo) {
        return Arrays.stream(tipo.getEnumConstants())
            .map(opcao -> new OpcaoDTO(opcao.name(), opcao.getRotulo()))
            .toList();
    }
}