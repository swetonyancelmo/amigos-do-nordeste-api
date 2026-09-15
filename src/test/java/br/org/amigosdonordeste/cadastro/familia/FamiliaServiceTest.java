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
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao;
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao.AtualizarFonteRenda;
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao.AtualizarPessoa;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao.CriarFonteRenda;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao.CriarPessoa;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
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
        CriarPessoa pessoa = new CriarPessoa(
                "Venicius Rafael", null, Sexo.MASCULINO, null,
                4, LocalDate.of(2026, 9, 4), null, true, Serie.PRE, null, null, null, null);

        CriarFonteRenda fonte = new CriarFonteRenda(
                TipoFonteRenda.BOLSA_FAMILIA, null, FaixaRenda.ATE_1_SALARIO, null);

        CriarFamiliaRequisicao request = new CriarFamiliaRequisicao(
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
    @DisplayName("fontesRenda.pessoaIndice amarra a fonte à pessoa daquela posição em pessoas[]")
    void amarraFonteDeRendaPelaPosicaoNoPayload() {
        CriarPessoa josefa = new CriarPessoa(
                "Josefa", null, Sexo.FEMININO, null, 40, LocalDate.now(), null, null, null, null, null, null, null);
        CriarPessoa antonio = new CriarPessoa(
                "Antônio", null, Sexo.MASCULINO, null, 70, LocalDate.now(), null, null, null, null, null, null, null);

        CriarFonteRenda bolsaDaFamilia = new CriarFonteRenda(
                TipoFonteRenda.BOLSA_FAMILIA, null, FaixaRenda.ATE_1_SALARIO, null);
        CriarFonteRenda aposentadoriaDoAntonio = new CriarFonteRenda(
                TipoFonteRenda.APOSENTADORIA, 1, FaixaRenda.ATE_1_SALARIO, null);

        CriarFamiliaRequisicao request = new CriarFamiliaRequisicao(
                comunidadeExistente.getId(), "Josefa", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(josefa, antonio), List.of(bolsaDaFamilia, aposentadoriaDoAntonio), null);

        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));
        when(familiaRepositorio.save(any(Familia.class))).thenAnswer(chamada -> {
            // simula o banco gerando os ids no INSERT
            Familia salva = chamada.getArgument(0);
            salva.getPessoas().forEach(p -> p.setId(UUID.randomUUID()));
            return salva;
        });

        FamiliaResponse resposta = familiaService.criar(request);

        UUID idDoAntonio = resposta.pessoas().stream()
                .filter(p -> "Antônio".equals(p.nome())).findFirst().orElseThrow().id();
        assertNull(resposta.fontesRenda().get(0).pessoaId());
        assertEquals(idDoAntonio, resposta.fontesRenda().get(1).pessoaId());
    }

    @Test
    @DisplayName("fontesRenda.pessoaIndice fora de pessoas[] é rejeitado")
    void rejeitaPessoaIndiceForaDoPayload() {
        CriarFonteRenda fonte = new CriarFonteRenda(
                TipoFonteRenda.BOLSA_FAMILIA, 0, FaixaRenda.ATE_1_SALARIO, null);

        CriarFamiliaRequisicao request = new CriarFamiliaRequisicao(
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
        CriarFamiliaRequisicao request = new CriarFamiliaRequisicao(
                comunidadeInexistente, "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(), List.of(), null);

        when(comunidadeRepositorio.findById(comunidadeInexistente)).thenReturn(Optional.empty());

        assertThrows(ComunidadeNaoEncontradaException.class, () -> familiaService.criar(request));
    }

    @Test
    @DisplayName("numeroCalcado fora da lista de NumerosCalcado.VALORES é rejeitado")
    void rejeitaNumeroCalcadoInvalido() {
        CriarPessoa pessoa = new CriarPessoa(
                "Venicius", null, Sexo.MASCULINO, null, 4, LocalDate.now(),
                null, true, Serie.PRE, null, "26", null, null); // "26" não existe na lista, só "26/27"

        CriarFamiliaRequisicao request = new CriarFamiliaRequisicao(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(pessoa), List.of(), null);

        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        assertThrows(NumeroCalcadoInvalidoException.class, () -> familiaService.criar(request));
    }

    @Test
    @DisplayName("idadeEstimadaEm sem idadeEstimada é rejeitado (espelha chk_pessoa_idade)")
    void rejeitaIdadeEstimadaEmSemIdadeEstimada() {
        CriarPessoa pessoa = new CriarPessoa(
                "Venicius", null, Sexo.MASCULINO, null, null, LocalDate.now(),
                null, true, Serie.PRE, null, null, null, null);

        CriarFamiliaRequisicao request = new CriarFamiliaRequisicao(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(pessoa), List.of(), null);

        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        assertThrows(IdadeEstimadaInvalidaException.class, () -> familiaService.criar(request));
    }

    @Test
    @DisplayName("pessoa sem nome e sem nenhuma informação de idade fica marcada como cadastro incompleto (RF-09)")
    void marcaCadastroIncompletoQuandoFaltaNomeEIdade() {
        CriarPessoa pessoaSemNadaQuaseNada = new CriarPessoa(
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        CriarFamiliaRequisicao request = new CriarFamiliaRequisicao(
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

        AtualizarFamiliaRequisicao requestQualquer = new AtualizarFamiliaRequisicao(
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

        AtualizarPessoa anaComNomeAtualizado = new AtualizarPessoa(
                ana.getId(), "Ana Paula", null, null, null, null, null, null, null, null, null, null, null, null);
        AtualizarPessoa brunoIntacto = new AtualizarPessoa(
                bruno.getId(), "Bruno", null, null, null, null, null, null, null, null, null, null, null, null);

        AtualizarFamiliaRequisicao request = new AtualizarFamiliaRequisicao(
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
        AtualizarPessoa brunoIntacto = new AtualizarPessoa(
                bruno.getId(), "Bruno", null, null, null, null, null, null, null, null, null, null, null, null);

        AtualizarFamiliaRequisicao request = new AtualizarFamiliaRequisicao(
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
        AtualizarFonteRenda fonteSemPessoa = new AtualizarFonteRenda(
                aposentadoria.getId(), TipoFonteRenda.APOSENTADORIA, null, FaixaRenda.DE_1_A_2_SALARIOS, null);

        AtualizarFamiliaRequisicao request = new AtualizarFamiliaRequisicao(
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
        AtualizarPessoa primeira = new AtualizarPessoa(
                mesmoId, "Ana", null, null, null, null, null, null, null, null, null, null, null, null);
        AtualizarPessoa segunda = new AtualizarPessoa(
                mesmoId, "Bruno", null, null, null, null, null, null, null, null, null, null, null, null);

        AtualizarFamiliaRequisicao request = new AtualizarFamiliaRequisicao(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(primeira, segunda), List.of(), null);

        assertThrows(IdDuplicadoNoPayloadException.class,
                () -> familiaService.atualizar(UUID.randomUUID(), request));
        verify(familiaRepositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("PUT com fontesRenda.pessoaId de pessoa que não é desta família é rejeitado")
    void rejeitaPessoaIdDeOutraFamiliaNoPut() {
        Familia familiaExistente = new Familia();
        familiaExistente.setId(UUID.randomUUID());
        familiaExistente.setComunidade(comunidadeExistente);

        when(familiaRepositorio.findById(familiaExistente.getId())).thenReturn(Optional.of(familiaExistente));
        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));

        AtualizarFonteRenda fonteDePessoaAlheia = new AtualizarFonteRenda(
                null, TipoFonteRenda.APOSENTADORIA, UUID.randomUUID(), FaixaRenda.ATE_1_SALARIO, null);

        AtualizarFamiliaRequisicao request = new AtualizarFamiliaRequisicao(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(), List.of(fonteDePessoaAlheia), null);

        assertThrows(PessoaReferenciadaInvalidaException.class,
                () -> familiaService.atualizar(familiaExistente.getId(), request));
        verify(familiaRepositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("CPF com máscara é gravado só com os dígitos")
    void normalizaCpfDoResponsavel() {
        when(comunidadeRepositorio.findById(comunidadeExistente.getId())).thenReturn(Optional.of(comunidadeExistente));
        when(familiaRepositorio.save(any(Familia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        CriarFamiliaRequisicao request = new CriarFamiliaRequisicao(
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

        AtualizarPessoa mesmaIdadeSemData = new AtualizarPessoa(
                avo.getId(), "Avô", null, null, null, 70, null, null, null, null, null, null, null, null);

        AtualizarFamiliaRequisicao request = new AtualizarFamiliaRequisicao(
                comunidadeExistente.getId(), "Maria", null, null, null, true,
                EscoamentoSanitario.FOSSA_RUDIMENTAR, TratamentoAgua.SEM_TRATAMENTO,
                Set.of(), List.of(mesmaIdadeSemData), List.of(), null);

        familiaService.atualizar(familiaExistente.getId(), request);

        assertEquals(dataOriginal, avo.getIdadeEstimadaEm());
    }
}
