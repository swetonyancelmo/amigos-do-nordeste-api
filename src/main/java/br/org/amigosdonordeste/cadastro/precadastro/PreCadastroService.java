package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.precadastro.dto.EnviarPreCadastroRequisicao;
import br.org.amigosdonordeste.cadastro.precadastro.dto.EnviarPreCadastroRequisicao.PessoaPreCadastro;
import br.org.amigosdonordeste.cadastro.precadastro.dto.EnviarPreCadastroResposta;
import br.org.amigosdonordeste.cadastro.precadastro.dto.ResultadoEnvio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.UUID;

/**
 * Recebe o pre-cadastro do aparelho da agente.
 *
 * A regra central: o mesmo id nunca grava duas vezes e nunca devolve erro.
 * Primeiro se pergunta ao banco se o id ja existe (o caso comum do reenvio);
 * se dois envios do mesmo id chegarem ao mesmo tempo e os dois passarem por
 * essa pergunta, a chave primaria segura o segundo INSERT e ele tambem vira
 * JA_RECEBIDO. E por isso que este service NAO e @Transactional: o INSERT
 * roda na transacao do repositorio e, se ela falhar por chave duplicada, nao
 * ha transacao de fora marcada para rollback.
 */
@Service
public class PreCadastroService {

    private final PreCadastroRepositorio preCadastros;
    private final AgenteRepositorio agentes;
    private final ComunidadeRepositorio comunidades;
    private final ObjectMapper json;
    private final Validator validador;

    public PreCadastroService(PreCadastroRepositorio preCadastros,
                              AgenteRepositorio agentes,
                              ComunidadeRepositorio comunidades,
                              ObjectMapper json,
                              Validator validador) {
        this.preCadastros = preCadastros;
        this.agentes = agentes;
        this.comunidades = comunidades;
        this.json = json;
        this.validador = validador;
    }

    /**
     * @param corpo o JSON como o aparelho mandou. E ele, e nao o DTO, que vai
     *              para pre_cadastro.payload: campo que o servidor ainda nao
     *              conhece tambem fica guardado para a revisao.
     */
    public EnviarPreCadastroResposta receber(UUID agenteId, JsonNode corpo) {
        EnviarPreCadastroRequisicao requisicao = converter(corpo);

        if (preCadastros.existsById(requisicao.id())) {
            return new EnviarPreCadastroResposta(requisicao.id(), ResultadoEnvio.JA_RECEBIDO);
        }

        requisicao.pessoas().forEach(PreCadastroService::validarPessoa);

        PreCadastro preCadastro = new PreCadastro();
        preCadastro.setId(requisicao.id());
        preCadastro.setAgente(agentes.getReferenceById(agenteId));
        if (requisicao.comunidadeId() != null) {
            // Comunidade desconhecida nao e erro: quem revisa resolve pelo
            // comunidadeNome que ficou no payload.
            comunidades.findById(requisicao.comunidadeId()).ifPresent(preCadastro::setComunidade);
        }
        preCadastro.setPayload(corpo.toString());

        try {
            preCadastros.saveAndFlush(preCadastro);
        } catch (DataIntegrityViolationException chaveDuplicada) {
            // Corrida entre dois envios do mesmo id: o outro chegou primeiro.
            if (preCadastros.existsById(requisicao.id())) {
                return new EnviarPreCadastroResposta(requisicao.id(), ResultadoEnvio.JA_RECEBIDO);
            }
            throw chaveDuplicada;
        }

        return new EnviarPreCadastroResposta(requisicao.id(), ResultadoEnvio.ACEITO);
    }

    /**
     * Duas regras que o Bean Validation nao expressa:
     *  - nome so pode faltar em cadastro incompleto (RF-09);
     *  - idadeEstimada e idadeEstimadaEm andam juntos: um sem o outro e
     *    recusado. Aqui nao se assume "hoje" como no POST /api/familias — o
     *    aparelho sabe quando estimou, e o envio pode acontecer dias depois.
     *    E a data sozinha nao vale nada; se passasse, a aprovacao travaria no
     *    chk_pessoa_idade sem a agente ter como corrigir.
     */
    private static void validarPessoa(PessoaPreCadastro pessoa) {
        boolean semNome = pessoa.nome() == null || pessoa.nome().isBlank();
        if (semNome && !Boolean.TRUE.equals(pessoa.cadastroIncompleto())) {
            throw new PreCadastroInvalidoException(
                "pessoas: nome só pode faltar quando cadastroIncompleto é true.");
        }
        if ((pessoa.idadeEstimada() == null) != (pessoa.idadeEstimadaEm() == null)) {
            throw new PreCadastroInvalidoException(
                "pessoas: idadeEstimada e idadeEstimadaEm precisam vir juntos.");
        }
    }

    /**
     * Faz a mao o que @Valid @RequestBody faria: converte o JSON no DTO e roda
     * o Bean Validation. Erro de formato (UUID, data, enum) e de constraint
     * viram o mesmo 400 dos outros endpoints.
     */
    private EnviarPreCadastroRequisicao converter(JsonNode corpo) {
        EnviarPreCadastroRequisicao requisicao;
        try {
            requisicao = json.treeToValue(corpo, EnviarPreCadastroRequisicao.class);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            throw new PreCadastroInvalidoException("Corpo do pré-cadastro inválido: campo com formato errado.");
        }
        validador.validate(requisicao).stream()
            .min(Comparator.comparing(v -> v.getPropertyPath().toString()))
            .ifPresent(v -> {
                throw new PreCadastroInvalidoException(v.getPropertyPath() + ": " + v.getMessage());
            });
        return requisicao;
    }
}
