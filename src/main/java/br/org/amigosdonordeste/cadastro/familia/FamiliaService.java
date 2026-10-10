package br.org.amigosdonordeste.cadastro.familia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.amigosdonordeste.cadastro.comum.dto.PaginaResposta;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.dominio.Cpf;
import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaFiltroDTO;
import br.org.amigosdonordeste.cadastro.familia.dto.OrdenacaoFamilia;
import br.org.amigosdonordeste.cadastro.familia.exception.CpfInvalidoException;
import br.org.amigosdonordeste.cadastro.familia.exception.CpfJaCadastradoException;
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
import br.org.amigosdonordeste.cadastro.vulnerabilidade.AvaliacaoVulnerabilidadeService;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Avaliacao;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;

@Service
@Transactional
public class FamiliaService {

    private final FamiliaRepositorio familiaRepositorio;
    private final ComunidadeRepositorio comunidadeRepositorio;
    // campos de pessoa e remoção de membro: a mesma regra da tela de pessoa
    private final PessoaService pessoaService;
    // sugestão de prioridade (ADR-0010): só lida, nunca gravada
    private final AvaliacaoVulnerabilidadeService avaliacaoVulnerabilidade;

    /** Ids por consulta "in (...)" ao avaliar a lista inteira. */
    private static final int LOTE_AVALIACAO = 500;

    public FamiliaService(FamiliaRepositorio familiaRepositorio, ComunidadeRepositorio comunidadeRepositorio,
                          PessoaService pessoaService, AvaliacaoVulnerabilidadeService avaliacaoVulnerabilidade) {
        this.familiaRepositorio = familiaRepositorio;
        this.comunidadeRepositorio = comunidadeRepositorio;
        this.pessoaService = pessoaService;
        this.avaliacaoVulnerabilidade = avaliacaoVulnerabilidade;
    }

    /**
     * Issue #17: a ficha completa da tela de edicao. readOnly porque aqui so
     * se le — evita que o Hibernate faca dirty checking do grafo inteiro.
     */
    @Transactional(readOnly = true)
    public FamiliaDetalheResponse buscarPorId(UUID id) {
        Familia familia = familiaRepositorio.buscarDetalhePorId(id)
                .orElseThrow(() -> new FamiliaNaoEncontradaException(id));
        return FamiliaDetalheResponse.fromEntity(familia, avaliacaoVulnerabilidade.avaliar(familia));
    }

    /**
     * Issue #16 (filtros, paginação, totais) e #43 (inativas só com
     * incluirInativas=true). Três consultas, qualquer que seja o tamanho da
     * página: a página filtrada, o count e comunidade/município/pessoas das
     * famílias da página (buscarComPessoasPorIds).
     */
    @Transactional(readOnly = true)
    public PaginaResposta<FamiliaResumoResponse> listar(FamiliaFiltroDTO filtro) {
        if (filtro.dependeDaAvaliacao()) {
            return listarPorAvaliacao(filtro);
        }

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
        BaseDeConhecimento base = avaliacaoVulnerabilidade.carregarBase();
        LocalDate hoje = LocalDate.now();
        List<FamiliaResumoResponse> itens = ids.stream()
                .map(carregadasPorId::get)
                .map(f -> FamiliaResumoResponse.fromEntity(f, AvaliacaoVulnerabilidadeService.avaliar(f, base, hoje)))
                .toList();
        return PaginaResposta.de(pagina, itens);
    }

    /**
     * Filtro ou ordem por estrato (ADR-0010). O estrato é calculado, nunca
     * coluna, então o banco não filtra nem ordena por ele: aqui se avaliam
     * todas as famílias que passam pelos outros filtros e a página é cortada
     * em memória. Com ~2.500 famílias, são algumas dezenas de consultas
     * pequenas (pessoas em lotes; fontes e abastecimento pelo @BatchSize).
     */
    private PaginaResposta<FamiliaResumoResponse> listarPorAvaliacao(FamiliaFiltroDTO filtro) {
        List<UUID> ids = familiaRepositorio
                .findAll(FamiliaEspecificacao.comFiltro(filtro), Sort.by("responsavelNome", "id"))
                .stream()
                .map(Familia::getId)
                .toList();

        BaseDeConhecimento base = avaliacaoVulnerabilidade.carregarBase();
        LocalDate hoje = LocalDate.now();
        Map<UUID, Familia> carregadasPorId = new HashMap<>();
        for (int i = 0; i < ids.size(); i += LOTE_AVALIACAO) {
            List<UUID> lote = ids.subList(i, Math.min(i + LOTE_AVALIACAO, ids.size()));
            familiaRepositorio.buscarComPessoasPorIds(lote).forEach(f -> carregadasPorId.put(f.getId(), f));
        }

        Set<EstratoRisco> estratos = filtro.estrato() == null || filtro.estrato().isEmpty()
                ? null
                : Set.copyOf(filtro.estrato());
        List<FamiliaResumoResponse> avaliadas = new ArrayList<>();
        Map<UUID, Avaliacao> avaliacoes = new HashMap<>();
        for (UUID id : ids) {
            Familia familia = carregadasPorId.get(id);
            Avaliacao avaliacao = AvaliacaoVulnerabilidadeService.avaliar(familia, base, hoje);
            if (estratos == null || estratos.contains(avaliacao.estrato())) {
                avaliadas.add(FamiliaResumoResponse.fromEntity(familia, avaliacao));
                avaliacoes.put(id, avaliacao);
            }
        }

        if (filtro.ordenacao() == OrdenacaoFamilia.PRIORIDADE) {
            // ordem dos estratos na base; dentro dele, mais pontos confirmados
            // primeiro; empate fica na ordem por nome que já veio do banco
            avaliadas.sort(Comparator
                    .comparingInt((FamiliaResumoResponse r) -> base.corte(r.vulnerabilidade().estrato()).ordem())
                    .thenComparing(r -> -avaliacoes.get(r.id()).pontosConfirmados()));
        }

        PageRequest pageable = PageRequest.of(filtro.paginaNormalizada(), filtro.porPaginaNormalizada());
        int inicio = (int) Math.min(pageable.getOffset(), avaliadas.size());
        int fim = Math.min(inicio + pageable.getPageSize(), avaliadas.size());
        List<FamiliaResumoResponse> itens = avaliadas.subList(inicio, fim);
        return PaginaResposta.de(new PageImpl<>(itens, pageable, avaliadas.size()), itens);
    }

    /**
     * Issue #43: exclusão física não existe — levaria junto pessoas, fontes de
     * renda e o histórico de contagem. Inativar de novo uma inativa não é erro.
     */
    public FamiliaResumoResponse inativar(UUID id) {
        Familia familia = buscarParaAlterar(id);
        familia.inativar();
        return FamiliaResumoResponse.fromEntity(familia, avaliacaoVulnerabilidade.avaliar(familia));
    }

    /** Issue #43: desfaz inativar(). Reativar uma ativa não é erro. */
    public FamiliaResumoResponse reativar(UUID id) {
        Familia familia = buscarParaAlterar(id);
        familia.reativar();
        return FamiliaResumoResponse.fromEntity(familia, avaliacaoVulnerabilidade.avaliar(familia));
    }

    private Familia buscarParaAlterar(UUID id) {
        return familiaRepositorio.findById(id)
                .orElseThrow(() -> new FamiliaNaoEncontradaException(id));
    }

    /** Issue #14 */
    public FamiliaResponse criar(CriarFamiliaRequisicao request) {
        validarCpf(request.responsavelCpf(), null);
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
        validarCpf(request.responsavelCpf(), id);

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
     * CPF é opcional, mas se vier tem que ser válido e não pode ser de outra
     * família, ativa ou inativa (uma inativa se reativa, não se recadastra).
     * Roda antes de mexer na entidade. Vale também para a aprovação de
     * pré-cadastro, que cria a família por criar().
     *
     * @param idDaFamilia null no POST; no PUT, a própria família não conta
     */
    private void validarCpf(String cpfInformado, UUID idDaFamilia) {
        String cpf = Cpf.somenteDigitos(cpfInformado);
        if (cpf == null) {
            return;
        }
        if (!Cpf.valido(cpf)) {
            throw new CpfInvalidoException();
        }
        Optional<Familia> outra = idDaFamilia == null
                ? familiaRepositorio.findFirstByResponsavelCpf(cpf)
                : familiaRepositorio.findFirstByResponsavelCpfAndIdNot(cpf, idDaFamilia);
        outra.ifPresent(f -> {
            throw new CpfJaCadastradoException(f.getResponsavelNome(), f.isAtiva());
        });
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
        familia.setResponsavelCpf(Cpf.somenteDigitos(request.responsavelCpf()));
        familia.setTelefone(request.telefone());
        familia.setPontoReferencia(request.pontoReferencia());
        familia.setTemBanheiro(request.temBanheiro());
        familia.setEscoamentoSanitario(request.escoamentoSanitario());
        familia.setTratamentoAgua(request.tratamentoAgua());
        familia.setFaixaRenda(request.faixaRenda());
        familia.setObservacoes(request.observacoes());
        familia.setNumeroComodos(request.numeroComodos());

        familia.getAbastecimentoAgua().clear();
        if (request.abastecimentoAgua() != null) {
            familia.getAbastecimentoAgua().addAll(request.abastecimentoAgua());
        }
    }
}
