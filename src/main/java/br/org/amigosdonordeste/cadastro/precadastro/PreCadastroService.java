package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.precadastro.dto.EnviarPreCadastroRequisicao;
import br.org.amigosdonordeste.cadastro.precadastro.dto.EnviarPreCadastroRequisicao.PessoaPreCadastro;
import br.org.amigosdonordeste.cadastro.precadastro.dto.EnviarPreCadastroResposta;
import br.org.amigosdonordeste.cadastro.precadastro.dto.PossivelDuplicata;
import br.org.amigosdonordeste.cadastro.precadastro.dto.PreCadastroResumoResposta;
import br.org.amigosdonordeste.cadastro.precadastro.dto.ResultadoEnvio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Recebe o pre-cadastro do aparelho da agente e lista a fila para o painel.
 *
 * A regra central do recebimento: o mesmo id nunca grava duas vezes e nunca devolve erro.
 * Primeiro se pergunta ao banco se o id ja existe (o caso comum do reenvio);
 * se dois envios do mesmo id chegarem ao mesmo tempo e os dois passarem por
 * essa pergunta, a chave primaria segura o segundo INSERT e ele tambem vira
 * JA_RECEBIDO. E por isso que este service NAO e @Transactional: o INSERT
 * roda na transacao do repositorio e, se ela falhar por chave duplicada, nao
 * ha transacao de fora marcada para rollback. (A listagem, que so le, abre
 * a sua propria transacao readOnly.)
 */
@Service
public class PreCadastroService {

    private final PreCadastroRepositorio preCadastros;
    private final AgenteRepositorio agentes;
    private final ComunidadeRepositorio comunidades;
    private final FamiliaRepositorio familias;
    private final ObjectMapper json;
    private final Validator validador;

    public PreCadastroService(PreCadastroRepositorio preCadastros,
                              AgenteRepositorio agentes,
                              ComunidadeRepositorio comunidades,
                              FamiliaRepositorio familias,
                              ObjectMapper json,
                              Validator validador) {
        this.preCadastros = preCadastros;
        this.agentes = agentes;
        this.comunidades = comunidades;
        this.familias = familias;
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
     * Lista da tela de Chamados. Tudo que a linha mostra sai do payload
     * guardado (nome, telefone, pessoas) ou das relacoes (agente, comunidade);
     * o total de pessoas e contado, nao lido de coluna.
     *
     * @param situacao filtro; nula lista todas as situacoes.
     */
    @Transactional(readOnly = true)
    public List<PreCadastroResumoResposta> listar(SituacaoPreCadastro situacao) {
        List<PreCadastro> fila = situacao == null
            ? preCadastros.listarTodos()
            : preCadastros.listarPorSituacao(situacao);
        return fila.stream().map(this::resumir).toList();
    }

    private PreCadastroResumoResposta resumir(PreCadastro preCadastro) {
        JsonNode payload = lerPayload(preCadastro);
        String responsavelNome = texto(payload, "responsavelNome");
        String telefone = texto(payload, "telefone");
        JsonNode pessoas = payload.path("pessoas");

        Comunidade comunidade = preCadastro.getComunidade();
        String comunidadeNome = comunidade != null ? comunidade.getNome() : texto(payload, "comunidadeNome");

        return new PreCadastroResumoResposta(
            preCadastro.getId(),
            responsavelNome,
            comunidade != null ? comunidade.getId() : null,
            comunidadeNome,
            pessoas.isArray() ? pessoas.size() : 0,
            preCadastro.getAgente().getNome(),
            preCadastro.getRecebidoEm(),
            preCadastro.getSituacao(),
            procurarDuplicata(preCadastro, responsavelNome, telefone));
    }

    /**
     * Duplicata e o risco numero um da coleta em campo: duas agentes, ou a
     * mesma em duas visitas, cadastrando a mesma familia. Procura dentro da
     * comunidade: primeiro por telefone (sinal mais forte), depois por nome
     * sem acento e sem maiuscula. Nunca bloqueia — so avisa.
     *
     * So vale para PENDENTE: um aprovado ja virou familia e apontaria a si
     * mesmo; um devolvido nao esta mais na fila. Sem comunidade reconhecida
     * nao ha onde procurar. Nos dois casos a resposta e null.
     */
    private PossivelDuplicata procurarDuplicata(PreCadastro preCadastro, String responsavelNome, String telefone) {
        if (preCadastro.getSituacao() != SituacaoPreCadastro.PENDENTE || preCadastro.getComunidade() == null) {
            return null;
        }
        UUID comunidadeId = preCadastro.getComunidade().getId();

        // Telefone comparado so pelos digitos, dos dois lados: o cadastrado
        // pode estar como "(87) 99999-0000", "87.99999.0000" ou "+5587...".
        // Feito em Java porque o regexp_replace do Postgres e o do H2 divergem.
        String soDigitos = soDigitos(telefone);
        if (!soDigitos.isEmpty()) {
            for (Familia familia : familias.findByComunidadeIdOrderByResponsavelNomeAsc(comunidadeId)) {
                if (soDigitos.equals(soDigitos(familia.getTelefone()))) {
                    return duplicata(familia, MotivoDuplicata.TELEFONE_IGUAL);
                }
            }
        }
        if (responsavelNome != null && !responsavelNome.isBlank()) {
            List<Familia> porNome = familias.buscarPorNomeParecidoNaComunidade(comunidadeId, responsavelNome);
            if (!porNome.isEmpty()) {
                return duplicata(porNome.get(0), MotivoDuplicata.NOME_PARECIDO);
            }
        }
        return null;
    }

    private static String soDigitos(String telefone) {
        return telefone == null ? "" : telefone.replaceAll("\\D", "");
    }

    private static PossivelDuplicata duplicata(Familia familia, MotivoDuplicata motivo) {
        return new PossivelDuplicata(familia.getId(), familia.getResponsavelNome(), motivo);
    }

    private JsonNode lerPayload(PreCadastro preCadastro) {
        try {
            return json.readTree(preCadastro.getPayload());
        } catch (JsonProcessingException e) {
            // O payload foi validado ao entrar; se nao for JSON agora, o banco foi editado na mao.
            throw new IllegalStateException("Payload do pré-cadastro " + preCadastro.getId() + " não é JSON.", e);
        }
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode valor = no.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
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
