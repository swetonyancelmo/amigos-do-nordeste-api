package br.org.amigosdonordeste.cadastro.agente;

import br.org.amigosdonordeste.cadastro.agente.dto.AgenteResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Lado do painel: cadastrar a agente e gerar o codigo de convite que ela
 * digita no app (a troca pelo token fica no AtivacaoAgenteService).
 *
 * Reemitir o convite usa a MESMA agente, nao uma nova: os pre-cadastros
 * ficam ligados a ela, e e por ela que o aparelho consulta a situacao do
 * que enviou. O token antigo deixa de valer na hora — e o caminho para um
 * celular perdido ou trocado.
 */
@Service
public class ConviteAgenteService {

    private static final int TENTATIVAS_CODIGO = 20;

    private final AgenteRepositorio agentes;
    private final SecureRandom aleatorio = new SecureRandom();

    public ConviteAgenteService(AgenteRepositorio agentes) {
        this.agentes = agentes;
    }

    @Transactional
    public AgenteResposta criar(String nome) {
        Agente agente = new Agente();
        agente.setNome(nome.trim());
        agente.setCodigoConvite(codigoLivre());
        return AgenteResposta.de(agentes.save(agente));
    }

    @Transactional(readOnly = true)
    public List<AgenteResposta> listar() {
        return agentes.findAllByOrderByNomeAsc().stream().map(AgenteResposta::de).toList();
    }

    @Transactional
    public AgenteResposta novoConvite(UUID id) {
        Agente agente = agentes.findById(id).orElseThrow(() -> new AgenteNaoEncontradoException(id));
        agente.setCodigoConvite(codigoLivre());
        agente.setTokenHash(null);
        agente.setAtivadoEm(null);
        agente.setAtivo(true);
        return AgenteResposta.de(agente);
    }

    /**
     * Seis digitos que nenhuma outra agente tem pendente. A coluna e UNIQUE,
     * entao uma corrida rarissima entre dois cadastros vira erro de
     * integridade (400), nunca dois aparelhos com o mesmo codigo.
     */
    private String codigoLivre() {
        for (int i = 0; i < TENTATIVAS_CODIGO; i++) {
            String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
            if (!agentes.existsByCodigoConvite(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código de convite livre.");
    }
}
