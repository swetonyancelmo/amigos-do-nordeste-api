package br.org.amigosdonordeste.cadastro.familia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.familia.exception.FamiliaNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdDuplicadoNoPayloadException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdadeEstimadaInvalidaException;
import br.org.amigosdonordeste.cadastro.familia.exception.NumeroCalcadoInvalidoException;
import br.org.amigosdonordeste.cadastro.familia.exception.PessoaReferenciadaInvalidaException;
import br.org.amigosdonordeste.cadastro.familia.request.FamiliaRequest;
import br.org.amigosdonordeste.cadastro.familia.request.FonteRendaRequest;
import br.org.amigosdonordeste.cadastro.familia.request.PessoaRequest;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;

@ExtendWith(MockitoExtension.class)
class FamiliaServiceTest {

    @Mock
    private FamiliaRepositorio familiaRepositorio;

    @Mock
    private ComunidadeRepositorio comunidadeRepositorio;

    @InjectMocks
    private FamiliaService familiaService;

    private Comunidade comunidadeExistente;

    @BeforeEach
    void setUp() {
        comunidadeExistente = new Comunidade();
        comunidadeExistente.setId(UUID.randomUUID());
    }

    // ---------- issue #14 (POST) ----------

    @Test
    @DisplayName("cria família com membro e fonte de renda numa única chamada")
    void criaFamiliaComMembroEFonte() {
        UUID idTemporarioDaPessoa = UUID.randomUUID();

        PessoaRequest pessoa = new PessoaRequest(
                idTemporarioDaPessoa, "Venicius Rafael", null, Sexo.MASCULINO, null,
                4, LocalDate.of(2026, 9, 4), null, true, "PRE", null, null, null, null);

        FonteRendaRequest fonte = new FonteRendaRequest(
                null, TipoFonteRenda.BOLSA_FAMILIA, null, FaixaRenda.ATE_1_SALARIO, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria Rizeuda da Silva", null, null,
                "Perto da igreja", true, EscoamentoSanitario.FOSSA_RUDIMENTAR,
                TratamentoAgua.SEM_TRATAMENTO, Set.of(AbastecimentoAgua.CISTERNA),
                List.of(pessoa), List.of(fonte), null);

        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));
        when(familiaRepositorio.save(any(Familia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        FamiliaResponse resposta = familiaService.criar(request);

        assertEquals(1, resposta.pessoas().size());
        assertEquals(1, resposta.fontesRenda().size());
        assertEquals(1, resposta.totais().totalPessoas());
        assertEquals(1, resposta.totais().totalFontesRenda());
        assertEquals(1, resposta.totais().totalPessoasEstudando());
    }

    @Test
    @DisplayName("fontesRenda.pessoaId que não bate com nenhuma pessoa do payload é rejeitado")
    void rejeitaPessoaIdInexistenteNoPayload() {
        FonteRendaRequest fonte = new FonteRendaRequest(
                null, TipoFonteRenda.BOLSA_FAMILIA, UUID.randomUUID(), FaixaRenda.ATE_1_SALARIO, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(), List.of(fonte), null);

        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        assertThrows(PessoaReferenciadaInvalidaException.class, () -> familiaService.criar(request));
        verify(familiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("comunidadeId inexistente é rejeitado")
    void rejeitaComunidadeInexistente() {
        UUID comunidadeInexistente = UUID.randomUUID();
        FamiliaRequest request = new FamiliaRequest(
                comunidadeInexistente, "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(), List.of(), null);

        when(comunidadeRepositorio.findById(comunidadeInexistente)).thenReturn(Optional.empty());

        assertThrows(ComunidadeNaoEncontradaException.class, () -> familiaService.criar(request));
    }

    @Test
    @DisplayName("numeroCalcado fora da lista de NumerosCalcado.VALORES é rejeitado")
    void rejeitaNumeroCalcadoInvalido() {
        PessoaRequest pessoa = new PessoaRequest(
                null, "Venicius", null, Sexo.MASCULINO, null, 4, LocalDate.now(),
                null, true, "PRE", null, "26", null, null); // "26" não existe na lista, só "26/27"

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(pessoa), List.of(), null);

        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        assertThrows(NumeroCalcadoInvalidoException.class, () -> familiaService.criar(request));
    }

    @Test
    @DisplayName("idadeEstimadaEm sem idadeEstimada é rejeitado (espelha chk_pessoa_idade)")
    void rejeitaIdadeEstimadaEmSemIdadeEstimada() {
        PessoaRequest pessoa = new PessoaRequest(
                null, "Venicius", null, Sexo.MASCULINO, null, null, LocalDate.now(),
                null, true, "PRE", null, null, null, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(pessoa), List.of(), null);

        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        assertThrows(IdadeEstimadaInvalidaException.class, () -> familiaService.criar(request));
    }

    @Test
    @DisplayName("pessoa sem nome e sem nenhuma informação de idade fica marcada como cadastro incompleto (RF-09)")
    void marcaCadastroIncompletoQuandoFaltaNomeEIdade() {
        PessoaRequest pessoaSemNadaQuaseNada = new PessoaRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(pessoaSemNadaQuaseNada), List.of(), null);

        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));
        when(familiaRepositorio.save(any(Familia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        FamiliaResponse resposta = familiaService.criar(request);

        assertTrue(resposta.pessoas().get(0).cadastroIncompleto());
    }

    // ---------- issue #15 (PUT) ----------

    @Test
    @DisplayName("família não encontrada é rejeitada")
    void rejeitaFamiliaNaoEncontrada() {
        UUID idInexistente = UUID.randomUUID();
        when(familiaRepositorio.findById(idInexistente)).thenReturn(Optional.empty());

        FamiliaRequest requestQualquer = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(), List.of(), null);

        assertThrows(FamiliaNaoEncontradaException.class,
                () -> familiaService.atualizar(idInexistente, requestQualquer));
    }

    @Test
    @DisplayName("editar o nome de um membro não afeta os outros")
    void editaUmMembroSemAfetarOutros() {
        Familia familiaExistente = new Familia();
        familiaExistente.setId(UUID.randomUUID());
        familiaExistente.setComunidade(comunidadeExistente);

        Pessoa ana = new Pessoa();
        ana.setId(UUID.randomUUID());
        ana.setNome("Ana");
        familiaExistente.adicionarPessoa(ana);

        Pessoa bruno = new Pessoa();
        bruno.setId(UUID.randomUUID());
        bruno.setNome("Bruno");
        familiaExistente.adicionarPessoa(bruno);

        when(familiaRepositorio.findById(familiaExistente.getId())).thenReturn(Optional.of(familiaExistente));
        when(familiaRepositorio.saveAndFlush(any(Familia.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        PessoaRequest anaComNomeAtualizado = new PessoaRequest(
                ana.getId(), "Ana Paula", null, null, null, null, null, null, null, null, null, null, null, null);
        PessoaRequest brunoIntacto = new PessoaRequest(
                bruno.getId(), "Bruno", null, null, null, null, null, null, null, null, null, null, null, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(anaComNomeAtualizado, brunoIntacto), List.of(), null);

        FamiliaResponse resposta = familiaService.atualizar(familiaExistente.getId(), request);

        assertEquals(2, resposta.pessoas().size());
        assertTrue(resposta.pessoas().stream().anyMatch(p -> "Ana Paula".equals(p.nome())));
        assertTrue(resposta.pessoas().stream().anyMatch(p -> "Bruno".equals(p.nome())));
    }

    @Test
    @DisplayName("remover uma pessoa do payload remove só ela")
    void removerPessoaDoPayloadRemoveApenasEla() {
        Familia familiaExistente = new Familia();
        familiaExistente.setId(UUID.randomUUID());
        familiaExistente.setComunidade(comunidadeExistente);

        Pessoa ana = new Pessoa();
        ana.setId(UUID.randomUUID());
        ana.setNome("Ana");
        familiaExistente.adicionarPessoa(ana);

        Pessoa bruno = new Pessoa();
        bruno.setId(UUID.randomUUID());
        bruno.setNome("Bruno");
        familiaExistente.adicionarPessoa(bruno);

        when(familiaRepositorio.findById(familiaExistente.getId())).thenReturn(Optional.of(familiaExistente));
        when(familiaRepositorio.saveAndFlush(any(Familia.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        // só Bruno no payload -> Ana deve sumir
        PessoaRequest brunoIntacto = new PessoaRequest(
                bruno.getId(), "Bruno", null, null, null, null, null, null, null, null, null, null, null, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(brunoIntacto), List.of(), null);

        FamiliaResponse resposta = familiaService.atualizar(familiaExistente.getId(), request);

        assertEquals(1, resposta.pessoas().size());
        assertEquals("Bruno", resposta.pessoas().get(0).nome());
    }

    @Test
    @DisplayName("fonte de renda de pessoa removida fica com pessoaId null, sem erro")
    void fonteDeRendaFicaSemPessoaQuandoPessoaERemovida() {
        Familia familiaExistente = new Familia();
        familiaExistente.setId(UUID.randomUUID());
        familiaExistente.setComunidade(comunidadeExistente);

        Pessoa avo = new Pessoa();
        avo.setId(UUID.randomUUID());
        avo.setNome("Avô");
        familiaExistente.adicionarPessoa(avo);

        FonteRenda aposentadoria = new FonteRenda();
        aposentadoria.setId(UUID.randomUUID());
        aposentadoria.setTipo(TipoFonteRenda.APOSENTADORIA);
        aposentadoria.setFaixa(FaixaRenda.DE_1_A_2_SALARIOS);
        aposentadoria.setPessoa(avo);
        familiaExistente.adicionarFonteRenda(aposentadoria);

        when(familiaRepositorio.findById(familiaExistente.getId())).thenReturn(Optional.of(familiaExistente));
        when(familiaRepositorio.saveAndFlush(any(Familia.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        // avô sumiu do payload; a fonte continua, mas sem pessoaId
        FonteRendaRequest fonteSemPessoa = new FonteRendaRequest(
                aposentadoria.getId(), TipoFonteRenda.APOSENTADORIA, null, FaixaRenda.DE_1_A_2_SALARIOS, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(), List.of(fonteSemPessoa), null);

        FamiliaResponse resposta = familiaService.atualizar(familiaExistente.getId(), request);

        assertTrue(resposta.pessoas().isEmpty());
        assertEquals(1, resposta.fontesRenda().size());
        assertNull(resposta.fontesRenda().get(0).pessoaId());
    }

    @Test
    @DisplayName("id repetido em pessoas[] é recusado")
    void rejeitaIdDePessoaDuplicadoNoPayload() {
        UUID mesmoId = UUID.randomUUID();
        PessoaRequest primeira = new PessoaRequest(
                mesmoId, "Ana", null, null, null, null, null, null, null, null, null, null, null, null);
        PessoaRequest segunda = new PessoaRequest(
                mesmoId, "Bruno", null, null, null, null, null, null, null, null, null, null, null, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(primeira, segunda), List.of(), null);

        assertThrows(IdDuplicadoNoPayloadException.class, () -> familiaService.criar(request));
        verify(familiaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("CPF com máscara é gravado só com os dígitos")
    void normalizaCpfDoResponsavel() {
        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));
        when(familiaRepositorio.save(any(Familia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", "000.000.000-00", null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(), List.of(), null);

        FamiliaResponse resposta = familiaService.criar(request);

        assertEquals("00000000000", resposta.responsavelCpf());
    }

    @Test
    @DisplayName("PUT sem idadeEstimadaEm mantém a data já gravada se a idade não mudou")
    void mantemIdadeEstimadaEmQuandoEstimativaNaoMuda() {
        Familia familiaExistente = new Familia();
        familiaExistente.setId(UUID.randomUUID());
        familiaExistente.setComunidade(comunidadeExistente);

        LocalDate dataOriginal = LocalDate.of(2024, 3, 10);
        Pessoa avo = new Pessoa();
        avo.setId(UUID.randomUUID());
        avo.setNome("Avô");
        avo.setIdadeEstimada(70);
        avo.setIdadeEstimadaEm(dataOriginal);
        familiaExistente.adicionarPessoa(avo);

        when(familiaRepositorio.findById(familiaExistente.getId())).thenReturn(Optional.of(familiaExistente));
        when(familiaRepositorio.saveAndFlush(any(Familia.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        PessoaRequest mesmaIdadeSemData = new PessoaRequest(
                avo.getId(), "Avô", null, null, null, 70, null, null, null, null, null, null, null, null);

        FamiliaRequest request = new FamiliaRequest(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(mesmaIdadeSemData), List.of(), null);

        familiaService.atualizar(familiaExistente.getId(), request);

        assertEquals(dataOriginal, avo.getIdadeEstimadaEm());
    }
}
