package br.org.amigosdonordeste.cadastro.familia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.amigosdonordeste.cadastro.comum.dto.PaginaResposta;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaFiltroDTO;
import br.org.amigosdonordeste.cadastro.familia.exception.FamiliaNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdDuplicadoNoPayloadException;
import br.org.amigosdonordeste.cadastro.familia.exception.PessoaReferenciadaInvalidaException;
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao;
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao.AtualizarFonteRenda;
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao.AtualizarPessoa;
import br.org.amigosdonordeste.cadastro.familia.request.CamposFamilia;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao.CriarFonteRenda;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao.CriarPessoa;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.PessoaService;
import br.org.amigosdonordeste.cadastro.pessoa.PessoaService.Origem;

@Service
@Transactional
public class FamiliaService {

    private final FamiliaRepositorio familiaRepositorio;
    private final ComunidadeRepositorio comunidadeRepositorio;
    // campos de pessoa e remoção de membro: a mesma regra da tela de pessoa
    private final PessoaService pessoaService;

    public FamiliaService(FamiliaRepositorio familiaRepositorio, ComunidadeRepositorio comunidadeRepositorio,
                          PessoaService pessoaService) {
        this.familiaRepositorio = familiaRepositorio;
        this.comunidadeRepositorio = comunidadeRepositorio;
        this.pessoaService = pessoaService;
    }

    /**
     * Issue #17: a ficha completa da tela de edicao. readOnly porque aqui so
     * se le — evita que o Hibernate faca dirty checking do grafo inteiro.
     */
    @Transactional(readOnly = true)
    public FamiliaDetalheResponse buscarPorId(UUID id) {
        Familia familia = familiaRepositorio.buscarDetalhePorId(id)
                .orElseThrow(() -> new FamiliaNaoEncontradaException(id));
        return FamiliaDetalheResponse.fromEntity(familia);
    }

    /**
     * Issue #16 (filtros, paginação, totais) e #43 (inativas só com
     * incluirInativas=true). Três consultas, qualquer que seja o tamanho da
     * página: a página filtrada, o count e comunidade/município/pessoas das
     * famílias da página (buscarComPessoasPorIds).
     */
    @Transactional(readOnly = true)
    public PaginaResposta<FamiliaResumoResponse> listar(FamiliaFiltroDTO filtro) {
        // id desempata nomes iguais — sem ele a mesma família pode aparecer
        // em duas páginas (ou em nenhuma)
        PageRequest pageable = PageRequest.of(
                filtro.paginaNormalizada(),
                filtro.porPaginaNormalizada(),
                Sort.by("responsavelNome", "id"));

        Page<Familia> pagina = familiaRepositorio.findAll(FamiliaEspecificacao.comFiltro(filtro), pageable);
        if (pagina.isEmpty()) {
            return PaginaResposta.de(pagina, List.of());
        }

        List<UUID> ids = pagina.map(Familia::getId).toList();
        Map<UUID, Familia> carregadasPorId = new HashMap<>();
        for (Familia familia : familiaRepositorio.buscarComPessoasPorIds(ids)) {
            carregadasPorId.put(familia.getId(), familia);
        }

        // devolve na ordem da página, não na ordem do "in (...)"
        List<FamiliaResumoResponse> itens = ids.stream()
                .map(carregadasPorId::get)
                .map(FamiliaResumoResponse::fromEntity)
                .toList();
        return PaginaResposta.de(pagina, itens);
    }

    /**
     * Issue #43: exclusão física não existe — levaria junto pessoas, fontes de
     * renda e o histórico de contagem. Inativar de novo uma inativa não é erro.
     */
    public FamiliaResumoResponse inativar(UUID id) {
        Familia familia = buscarParaAlterar(id);
        familia.inativar();
        return FamiliaResumoResponse.fromEntity(familia);
    }

    /** Issue #43: desfaz inativar(). Reativar uma ativa não é erro. */
    public FamiliaResumoResponse reativar(UUID id) {
        Familia familia = buscarParaAlterar(id);
        familia.reativar();
        return FamiliaResumoResponse.fromEntity(familia);
    }

    private Familia buscarParaAlterar(UUID id) {
        return familiaRepositorio.findById(id)
                .orElseThrow(() -> new FamiliaNaoEncontradaException(id));
    }

    /** Issue #14 */
    public FamiliaResponse criar(CriarFamiliaRequisicao request) {
        Familia familia = new Familia();
        familia.setComunidade(buscarComunidade(request.comunidadeId()));
        aplicarCamposSimples(familia, request);

        // no POST ninguém tem id ainda: fontesRenda[].pessoaIndice aponta
        // pra posição em pessoas[]
        List<Pessoa> pessoasNaOrdemDoPayload = new ArrayList<>();
        for (CriarPessoa pessoaRequest : request.pessoas()) {
            Pessoa pessoa = new Pessoa();
            pessoaService.aplicarCampos(pessoa, pessoaRequest, Origem.PAYLOAD_DA_FAMILIA);
            familia.adicionarPessoa(pessoa);
            pessoasNaOrdemDoPayload.add(pessoa);
        }

        for (CriarFonteRenda fonteRequest : request.fontesRenda()) {
            FonteRenda fonte = new FonteRenda();
            fonte.setTipo(fonteRequest.tipo());
            fonte.setObservacao(fonteRequest.observacao());
            fonte.setPessoa(resolverPessoaPorIndice(fonteRequest.pessoaIndice(), pessoasNaOrdemDoPayload));
            familia.adicionarFonteRenda(fonte);
        }

        return FamiliaResponse.fromEntity(familiaRepositorio.save(familia));
    }

    /** Issue #15 */
    public FamiliaResponse atualizar(UUID id, AtualizarFamiliaRequisicao request) {
        rejeitarIdsDuplicados(request);

        Familia familia = familiaRepositorio.findById(id)
                .orElseThrow(() -> new FamiliaNaoEncontradaException(id));

        familia.setComunidade(buscarComunidade(request.comunidadeId()));
        aplicarCamposSimples(familia, request);

        Map<UUID, Pessoa> pessoasAtuaisPorId = new HashMap<>();
        for (Pessoa pessoa : familia.getPessoas()) {
            pessoasAtuaisPorId.put(pessoa.getId(), pessoa);
        }

        Set<UUID> idsQueContinuam = new HashSet<>();

        for (AtualizarPessoa pessoaRequest : request.pessoas()) {
            if (pessoaRequest.id() != null && pessoasAtuaisPorId.containsKey(pessoaRequest.id())) {
                Pessoa existente = pessoasAtuaisPorId.get(pessoaRequest.id());
                pessoaService.aplicarCampos(existente, pessoaRequest, Origem.PAYLOAD_DA_FAMILIA);
                idsQueContinuam.add(existente.getId());
            } else {
                // id nulo ou de pessoa que não é desta família: cria nova, sem
                // aproveitar o id — é o que impede o JPA de "mover" uma pessoa
                // de outra família pra cá
                Pessoa nova = new Pessoa();
                pessoaService.aplicarCampos(nova, pessoaRequest, Origem.PAYLOAD_DA_FAMILIA);
                familia.adicionarPessoa(nova);
            }
        }

        // pessoa removida: a fonte de renda dela fica com a família (pessoa_id
        // = null) — mesma regra do DELETE /api/pessoas/{id}
        familia.getPessoas().stream()
                .filter(p -> p.getId() != null && !idsQueContinuam.contains(p.getId()))
                .toList()
                .forEach(pessoaRemovida -> pessoaService.removerDaFamilia(familia, pessoaRemovida));

        Map<UUID, FonteRenda> fontesAtuaisPorId = new HashMap<>();
        for (FonteRenda fonte : familia.getFontesRenda()) {
            fontesAtuaisPorId.put(fonte.getId(), fonte);
        }
        Set<UUID> idsFontesQueContinuam = new HashSet<>();

        for (AtualizarFonteRenda fonteRequest : request.fontesRenda()) {
            Pessoa pessoaDaFonte = resolverPessoaPorId(fonteRequest.pessoaId(), pessoasAtuaisPorId, idsQueContinuam);

            if (fonteRequest.id() != null && fontesAtuaisPorId.containsKey(fonteRequest.id())) {
                FonteRenda existente = fontesAtuaisPorId.get(fonteRequest.id());
                existente.setTipo(fonteRequest.tipo());
                existente.setObservacao(fonteRequest.observacao());
                existente.setPessoa(pessoaDaFonte);
                idsFontesQueContinuam.add(existente.getId());
            } else {
                FonteRenda nova = new FonteRenda();
                nova.setTipo(fonteRequest.tipo());
                nova.setObservacao(fonteRequest.observacao());
                nova.setPessoa(pessoaDaFonte);
                familia.adicionarFonteRenda(nova);
            }
        }

        familia.getFontesRenda().stream()
                .filter(f -> f.getId() != null && !idsFontesQueContinuam.contains(f.getId()))
                .toList()
                .forEach(familia::removerFonteRenda);

        // saveAndFlush em vez de confiar só no commit: as pessoas/fontes novas
        // só ganham id no INSERT, e a resposta precisa devolvê-los — senão o
        // front manda o membro sem id no próximo PUT e ele é criado de novo
        return FamiliaResponse.fromEntity(familiaRepositorio.saveAndFlush(familia));
    }

    /**
     * Dois itens com o mesmo id no payload cairiam no mesmo ramo "existente" e
     * o segundo sobrescreveria o primeiro em silêncio. Melhor recusar de cara.
     */
    private void rejeitarIdsDuplicados(AtualizarFamiliaRequisicao request) {
        Set<UUID> idsPessoas = new HashSet<>();
        for (AtualizarPessoa pessoa : request.pessoas()) {
            if (pessoa.id() != null && !idsPessoas.add(pessoa.id())) {
                throw new IdDuplicadoNoPayloadException("pessoas", pessoa.id());
            }
        }
        Set<UUID> idsFontes = new HashSet<>();
        for (AtualizarFonteRenda fonte : request.fontesRenda()) {
            if (fonte.id() != null && !idsFontes.add(fonte.id())) {
                throw new IdDuplicadoNoPayloadException("fontesRenda", fonte.id());
            }
        }
    }

    /** POST: a pessoa é a de posição pessoaIndice em pessoas[]. */
    private Pessoa resolverPessoaPorIndice(Integer pessoaIndice, List<Pessoa> pessoasNaOrdemDoPayload) {
        if (pessoaIndice == null) {
            return null;
        }
        if (pessoaIndice < 0 || pessoaIndice >= pessoasNaOrdemDoPayload.size()) {
            throw new PessoaReferenciadaInvalidaException(pessoaIndice, pessoasNaOrdemDoPayload.size());
        }
        return pessoasNaOrdemDoPayload.get(pessoaIndice);
    }

    /**
     * PUT: a pessoa tem que já existir nesta família e continuar no payload
     * (se saiu de pessoas[], ela vai ser removida — não dá pra amarrar fonte
     * a ela).
     */
    private Pessoa resolverPessoaPorId(UUID pessoaId, Map<UUID, Pessoa> pessoasAtuaisPorId, Set<UUID> idsQueContinuam) {
        if (pessoaId == null) {
            return null;
        }
        if (!idsQueContinuam.contains(pessoaId)) {
            throw new PessoaReferenciadaInvalidaException(pessoaId);
        }
        return pessoasAtuaisPorId.get(pessoaId);
    }

    private Comunidade buscarComunidade(UUID comunidadeId) {
        return comunidadeRepositorio.findById(comunidadeId)
                .orElseThrow(() -> new ComunidadeNaoEncontradaException(comunidadeId));
    }

    private void aplicarCamposSimples(Familia familia, CamposFamilia request) {
        familia.setResponsavelNome(request.responsavelNome());
        familia.setResponsavelCpf(somenteDigitos(request.responsavelCpf()));
        familia.setTelefone(request.telefone());
        familia.setPontoReferencia(request.pontoReferencia());
        familia.setTemBanheiro(request.temBanheiro());
        familia.setEscoamentoSanitario(request.escoamentoSanitario());
        familia.setTratamentoAgua(request.tratamentoAgua());
        familia.setFaixaRenda(request.faixaRenda());
        familia.setObservacoes(request.observacoes());

        familia.getAbastecimentoAgua().clear();
        if (request.abastecimentoAgua() != null) {
            familia.getAbastecimentoAgua().addAll(request.abastecimentoAgua());
        }
    }

    /** A coluna guarda só os 11 dígitos; o front pode mandar "000.000.000-00". */
    private static String somenteDigitos(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.replaceAll("\\D", "");
    }
}
