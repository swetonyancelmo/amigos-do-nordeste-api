package br.org.amigosdonordeste.cadastro.familia;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.dominio.NumerosCalcado;
import br.org.amigosdonordeste.cadastro.familia.exception.FamiliaNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdadeEstimadaInvalidaException;
import br.org.amigosdonordeste.cadastro.familia.exception.NumeroCalcadoInvalidoException;
import br.org.amigosdonordeste.cadastro.familia.exception.PessoaReferenciadaInvalidaException;
import br.org.amigosdonordeste.cadastro.familia.request.FamiliaRequest;
import br.org.amigosdonordeste.cadastro.familia.request.FonteRendaRequest;
import br.org.amigosdonordeste.cadastro.familia.request.PessoaRequest;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;

@Service
@Transactional
public class FamiliaService {

    private final FamiliaRepositorio familiaRepositorio;
    private final ComunidadeRepositorio comunidadeRepositorio;

    public FamiliaService(FamiliaRepositorio familiaRepositorio, ComunidadeRepositorio comunidadeRepositorio) {
        this.familiaRepositorio = familiaRepositorio;
        this.comunidadeRepositorio = comunidadeRepositorio;
    }

    /** Issue #14 */
    public FamiliaResponse criar(FamiliaRequest request) {
        Familia familia = new Familia();
        familia.setComunidade(buscarComunidade(request.comunidadeId()));
        aplicarCamposSimples(familia, request);

        Map<UUID, Pessoa> pessoasPorIdDoPayload = new HashMap<>();
        for (PessoaRequest pessoaRequest : request.pessoas()) {
            Pessoa pessoa = new Pessoa();
            aplicarCamposPessoa(pessoa, pessoaRequest);
            familia.adicionarPessoa(pessoa);
            if (pessoaRequest.id() != null) {
                pessoasPorIdDoPayload.put(pessoaRequest.id(), pessoa);
            }
        }

        for (FonteRendaRequest fonteRequest : request.fontesRenda()) {
            familia.adicionarFonteRenda(criarFonteRenda(fonteRequest, pessoasPorIdDoPayload));
        }

        return FamiliaResponse.fromEntity(familiaRepositorio.save(familia));
    }

    /** Issue #15 */
    public FamiliaResponse atualizar(UUID id, FamiliaRequest request) {
        Familia familia = familiaRepositorio.findById(id)
                .orElseThrow(() -> new FamiliaNaoEncontradaException(id));

        familia.setComunidade(buscarComunidade(request.comunidadeId()));
        aplicarCamposSimples(familia, request);

        Map<UUID, Pessoa> pessoasAtuaisPorId = new HashMap<>();
        for (Pessoa pessoa : familia.getPessoas()) {
            pessoasAtuaisPorId.put(pessoa.getId(), pessoa);
        }

        Set<UUID> idsQueContinuam = new HashSet<>();
        Map<UUID, Pessoa> pessoasPorIdDoPayload = new HashMap<>();

        for (PessoaRequest pessoaRequest : request.pessoas()) {
            if (pessoaRequest.id() != null && pessoasAtuaisPorId.containsKey(pessoaRequest.id())) {
                Pessoa existente = pessoasAtuaisPorId.get(pessoaRequest.id());
                aplicarCamposPessoa(existente, pessoaRequest);
                idsQueContinuam.add(existente.getId());
                pessoasPorIdDoPayload.put(pessoaRequest.id(), existente);
            } else {
                Pessoa nova = new Pessoa();
                aplicarCamposPessoa(nova, pessoaRequest);
                familia.adicionarPessoa(nova);
                if (pessoaRequest.id() != null) {
                    pessoasPorIdDoPayload.put(pessoaRequest.id(), nova);
                }
            }
        }

        // pessoa removida: desliga a fonte de renda dela antes (pessoa_id =
        // null) em vez de deixar o orphanRemoval tentar apagar a fonte junto
        familia.getPessoas().stream()
                .filter(p -> p.getId() != null && !idsQueContinuam.contains(p.getId()))
                .toList()
                .forEach(pessoaRemovida -> {
                    familia.getFontesRenda().stream()
                            .filter(f -> pessoaRemovida.equals(f.getPessoa()))
                            .forEach(f -> f.setPessoa(null));
                    familia.removerPessoa(pessoaRemovida);
                });

        Map<UUID, FonteRenda> fontesAtuaisPorId = new HashMap<>();
        for (FonteRenda fonte : familia.getFontesRenda()) {
            fontesAtuaisPorId.put(fonte.getId(), fonte);
        }
        Set<UUID> idsFontesQueContinuam = new HashSet<>();

        for (FonteRendaRequest fonteRequest : request.fontesRenda()) {
            Pessoa pessoaDaFonte = resolverPessoaDaFonte(fonteRequest, pessoasPorIdDoPayload);

            if (fonteRequest.id() != null && fontesAtuaisPorId.containsKey(fonteRequest.id())) {
                FonteRenda existente = fontesAtuaisPorId.get(fonteRequest.id());
                existente.setTipo(fonteRequest.tipo());
                existente.setFaixa(fonteRequest.faixa());
                existente.setObservacao(fonteRequest.observacao());
                existente.setPessoa(pessoaDaFonte);
                idsFontesQueContinuam.add(existente.getId());
            } else {
                FonteRenda nova = new FonteRenda();
                nova.setTipo(fonteRequest.tipo());
                nova.setFaixa(fonteRequest.faixa());
                nova.setObservacao(fonteRequest.observacao());
                nova.setPessoa(pessoaDaFonte);
                familia.adicionarFonteRenda(nova);
            }
        }

        familia.getFontesRenda().stream()
                .filter(f -> f.getId() != null && !idsFontesQueContinuam.contains(f.getId()))
                .toList()
                .forEach(familia::removerFonteRenda);

        // entidade gerenciada (mesmo esquema do ComunidadeService.atualizar):
        // o UPDATE sai no commit da transação, sem precisar de save()
        return FamiliaResponse.fromEntity(familia);
    }

    private FonteRenda criarFonteRenda(FonteRendaRequest fonteRequest, Map<UUID, Pessoa> pessoasPorIdDoPayload) {
        FonteRenda fonte = new FonteRenda();
        fonte.setTipo(fonteRequest.tipo());
        fonte.setFaixa(fonteRequest.faixa());
        fonte.setObservacao(fonteRequest.observacao());
        fonte.setPessoa(resolverPessoaDaFonte(fonteRequest, pessoasPorIdDoPayload));
        return fonte;
    }

    private Pessoa resolverPessoaDaFonte(FonteRendaRequest fonteRequest, Map<UUID, Pessoa> pessoasPorIdDoPayload) {
        if (fonteRequest.pessoaId() == null) {
            return null;
        }
        Pessoa pessoa = pessoasPorIdDoPayload.get(fonteRequest.pessoaId());
        if (pessoa == null) {
            throw new PessoaReferenciadaInvalidaException(fonteRequest.pessoaId());
        }
        return pessoa;
    }

    private Comunidade buscarComunidade(UUID comunidadeId) {
        return comunidadeRepositorio.findById(comunidadeId)
                .orElseThrow(() -> new ComunidadeNaoEncontradaException(comunidadeId));
    }

    private void aplicarCamposSimples(Familia familia, FamiliaRequest request) {
        familia.setResponsavelNome(request.responsavelNome());
        familia.setResponsavelCpf(request.responsavelCpf());
        familia.setTelefone(request.telefone());
        familia.setPontoReferencia(request.pontoReferencia());
        familia.setTemBanheiro(request.temBanheiro());
        familia.setEscoamentoSanitario(request.escoamentoSanitario());
        familia.setTratamentoAgua(request.tratamentoAgua());
        familia.setObservacoes(request.observacoes());

        familia.getAbastecimentoAgua().clear();
        if (request.abastecimentoAgua() != null) {
            familia.getAbastecimentoAgua().addAll(request.abastecimentoAgua());
        }
    }

    private void aplicarCamposPessoa(Pessoa pessoa, PessoaRequest request) {
    pessoa.setNome(request.nome());
    pessoa.setSexo(request.sexo());
    pessoa.setDataNascimento(request.dataNascimento());
    pessoa.setIdadeEstimada(request.idadeEstimada());

    // Regra da issue #14: idadeEstimada sem idadeEstimadaEm -> usa hoje.
    LocalDate dataEstimativa = request.idadeEstimadaEm();
    if (request.idadeEstimada() != null && dataEstimativa == null) {
        dataEstimativa = LocalDate.now();
    }
    pessoa.setIdadeEstimadaEm(dataEstimativa);

    // Espelha o chk_pessoa_idade: sem data de nascimento, os dois campos de
    // estimativa têm que vir juntos (o auto-preenchimento acima já cobre um
    // lado; aqui pegamos o caso de vir só a data, sem a idade).
    if (pessoa.getDataNascimento() == null
            && (pessoa.getIdadeEstimada() == null) != (pessoa.getIdadeEstimadaEm() == null)) {
        throw new IdadeEstimadaInvalidaException();
    }

    pessoa.setParentesco(request.parentesco());
    pessoa.setEstuda(request.estuda());
    pessoa.setSerie(request.serie());
    pessoa.setTamanhoRoupa(request.tamanhoRoupa());

    if (request.numeroCalcado() != null && !NumerosCalcado.ehValido(request.numeroCalcado())) {
        throw new NumeroCalcadoInvalidoException(request.numeroCalcado());
    }
    pessoa.setNumeroCalcado(request.numeroCalcado());

    pessoa.setGestante(request.gestante());
    pessoa.setObservacoes(request.observacoes());

    // RF-09: marca como incompleto se faltar nome ou faltar toda informação
    // de idade — além de respeitar se o cliente já mandou true explicitamente.
    // ASSUNÇÃO: confirma com quem escreveu a RF-09 se é exatamente essa a regra.
    boolean semNome = request.nome() == null || request.nome().isBlank();
    boolean semIdadeAlguma = pessoa.getDataNascimento() == null && pessoa.getIdadeEstimada() == null;
    pessoa.setCadastroIncompleto(semNome || semIdadeAlguma || Boolean.TRUE.equals(request.cadastroIncompleto()));
    }
}