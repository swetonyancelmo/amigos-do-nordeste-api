package br.org.amigosdonordeste.cadastro.pessoa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.amigosdonordeste.cadastro.comum.dto.PaginaResposta;
import br.org.amigosdonordeste.cadastro.dominio.NumerosCalcado;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.familia.exception.FamiliaNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdadeEstimadaInvalidaException;
import br.org.amigosdonordeste.cadastro.familia.exception.NumeroCalcadoInvalidoException;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.dto.PessoaFiltroDTO;
import br.org.amigosdonordeste.cadastro.pessoa.exception.PessoaInvalidaException;
import br.org.amigosdonordeste.cadastro.pessoa.exception.PessoaNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.pessoa.request.CamposPessoa;
import br.org.amigosdonordeste.cadastro.pessoa.request.PessoaRequisicao;

/**
 * O único lugar que grava campos de pessoa e que tira uma pessoa da família.
 * O payload da família (FamiliaService) e a tela de pessoa (PessoaController)
 * passam os dois por aqui — se houvesse duas cópias da regra de idade ou de
 * cadastro incompleto, um dia elas divergiriam e o relatório de roupa e
 * calçado sairia errado sem ninguém perceber.
 *
 * Nada de dado pessoal em log ou em mensagem de erro: as mensagens daqui vão
 * para a usuária e não citam nome, CPF nem telefone.
 */
@Service
@Transactional
public class PessoaService {

    /**
     * De onde vêm os campos. A regra é uma só; o que muda é quanto o servidor
     * completa sozinho versus quanto recusa.
     */
    public enum Origem {
        /**
         * Pessoa aninhada em POST/PUT /api/familias (e na aprovação de
         * pré-cadastro, que passa pelo POST). Contrato já publicado: idade
         * estimada sem data vira "estimada hoje" (issue #14), e nome em branco
         * só marca o cadastro como incompleto.
         */
        PAYLOAD_DA_FAMILIA,

        /**
         * Tela de pessoa. Aqui a tela tem como perguntar, então o que o
         * payload da família completa sozinho vira 400: estimativa sem data,
         * data de nascimento junto com estimativa, data no futuro, e nome
         * vazio sem cadastroIncompleto = true.
         */
        TELA_DE_PESSOA
    }

    private final PessoaRepositorio pessoaRepositorio;
    private final FamiliaRepositorio familiaRepositorio;

    public PessoaService(PessoaRepositorio pessoaRepositorio, FamiliaRepositorio familiaRepositorio) {
        this.pessoaRepositorio = pessoaRepositorio;
        this.familiaRepositorio = familiaRepositorio;
    }

    // ---------- tela de pessoa ----------

    @Transactional(readOnly = true)
    public PaginaResposta<PessoaResumoResponse> listar(PessoaFiltroDTO filtro) {
        // id desempata nomes iguais (e os sem nome) — sem ele a mesma pessoa
        // pode aparecer em duas páginas, ou em nenhuma
        PageRequest pageable = PageRequest.of(
                filtro.paginaNormalizada(),
                filtro.tamanhoNormalizado(),
                Sort.by("nome", "id"));

        Page<Pessoa> pagina = pessoaRepositorio.findAll(
                PessoaEspecificacao.comFiltro(filtro, LocalDate.now()), pageable);
        List<PessoaResumoResponse> itens = pagina.map(PessoaResumoResponse::fromEntity).toList();
        return PaginaResposta.de(pagina, itens);
    }

    @Transactional(readOnly = true)
    public PessoaDetalheResponse buscarPorId(UUID id) {
        return PessoaDetalheResponse.fromEntity(buscarDetalhe(id));
    }

    public PessoaDetalheResponse criar(UUID familiaId, PessoaRequisicao request) {
        Familia familia = familiaRepositorio.findById(familiaId)
                .orElseThrow(() -> new FamiliaNaoEncontradaException(familiaId));

        Pessoa pessoa = new Pessoa();
        aplicarCampos(pessoa, request, Origem.TELA_DE_PESSOA);
        familia.adicionarPessoa(pessoa);
        // saveAndFlush: o id só existe depois do INSERT, e a resposta o devolve
        return PessoaDetalheResponse.fromEntity(pessoaRepositorio.saveAndFlush(pessoa));
    }

    public PessoaDetalheResponse atualizar(UUID id, PessoaRequisicao request) {
        Pessoa pessoa = buscarDetalhe(id);
        aplicarCampos(pessoa, request, Origem.TELA_DE_PESSOA);
        return PessoaDetalheResponse.fromEntity(pessoa);
    }

    public void remover(UUID id) {
        Pessoa pessoa = pessoaRepositorio.findById(id)
                .orElseThrow(() -> new PessoaNaoEncontradaException(id));
        removerDaFamilia(pessoa.getFamilia(), pessoa);
    }

    private Pessoa buscarDetalhe(UUID id) {
        return pessoaRepositorio.buscarDetalhePorId(id)
                .orElseThrow(() -> new PessoaNaoEncontradaException(id));
    }

    // ---------- regra compartilhada com o FamiliaService ----------

    /**
     * Tira a pessoa da família (o orphanRemoval apaga a linha). A fonte de
     * renda dela continua, agora da família: pessoa_id = null antes, em vez
     * de deixar o banco ou o orphanRemoval decidirem.
     */
    public void removerDaFamilia(Familia familia, Pessoa pessoa) {
        familia.getFontesRenda().stream()
                .filter(fonte -> ehDaPessoa(fonte, pessoa))
                .forEach(fonte -> fonte.setPessoa(null));
        familia.removerPessoa(pessoa);
    }

    private static boolean ehDaPessoa(FonteRenda fonte, Pessoa pessoa) {
        Pessoa dono = fonte.getPessoa();
        if (dono == null) {
            return false;
        }
        // compara id além de instância: a fonte pode ter um proxy lazy da pessoa
        return dono == pessoa || (pessoa.getId() != null && pessoa.getId().equals(dono.getId()));
    }

    /**
     * Copia os campos para a entidade, validando. Única implementação das
     * regras de idade, calçado e cadastro incompleto.
     */
    public void aplicarCampos(Pessoa pessoa, CamposPessoa request, Origem origem) {
        if (origem == Origem.TELA_DE_PESSOA) {
            validarParaTelaDePessoa(request);
        }

        pessoa.setNome(request.nome());
        pessoa.setSexo(request.sexo());
        pessoa.setDataNascimento(request.dataNascimento());
        Integer idadeEstimadaAnterior = pessoa.getIdadeEstimada();
        LocalDate idadeEstimadaEmAnterior = pessoa.getIdadeEstimadaEm();
        pessoa.setIdadeEstimada(request.idadeEstimada());

        // Regra da issue #14: idadeEstimada sem idadeEstimadaEm -> usa hoje.
        // No PUT, se a estimativa não mudou e a data veio omitida, mantém a data
        // já gravada — senão "30 anos em 2024" viraria "30 anos em 2026" a cada
        // salvamento e a idade calculada regrediria.
        // (Na TELA_DE_PESSOA esse caso já foi recusado acima.)
        LocalDate dataEstimativa = request.idadeEstimadaEm();
        if (request.idadeEstimada() != null && dataEstimativa == null) {
            boolean estimativaInalterada = request.idadeEstimada().equals(idadeEstimadaAnterior)
                    && idadeEstimadaEmAnterior != null;
            dataEstimativa = estimativaInalterada ? idadeEstimadaEmAnterior : LocalDate.now();
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

    private static void validarParaTelaDePessoa(CamposPessoa request) {
        LocalDate hoje = LocalDate.now();
        boolean temEstimativa = request.idadeEstimada() != null || request.idadeEstimadaEm() != null;

        if (request.dataNascimento() != null && temEstimativa) {
            throw new PessoaInvalidaException(
                    "Informe a data de nascimento ou a idade estimada, não as duas.");
        }
        if (request.dataNascimento() != null && request.dataNascimento().isAfter(hoje)) {
            throw new PessoaInvalidaException("A data de nascimento não pode estar no futuro.");
        }
        if (request.idadeEstimada() != null && request.idadeEstimadaEm() == null) {
            throw new PessoaInvalidaException(
                    "Informe em que data a idade foi estimada (idadeEstimadaEm) — sem ela o sistema não "
                            + "consegue atualizar a idade com o passar dos anos.");
        }
        if (request.idadeEstimadaEm() != null && request.idadeEstimadaEm().isAfter(hoje)) {
            throw new PessoaInvalidaException("A data da estimativa de idade não pode estar no futuro.");
        }
        boolean semNome = request.nome() == null || request.nome().isBlank();
        if (semNome && !Boolean.TRUE.equals(request.cadastroIncompleto())) {
            throw new PessoaInvalidaException(
                    "Informe o nome, ou marque o cadastro como incompleto para salvar sem nome.");
        }
    }
}
