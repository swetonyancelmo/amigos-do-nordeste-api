package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que estes testes protegem (issue #17):
 *  - a ficha vem inteira: comunidade, municipio, membros e fontes de renda;
 *  - os totais sao contados na hora, inclusive por faixa etaria;
 *  - id inexistente e 404 com mensagem em portugues, nao 500;
 *  - comunidade, municipio e pessoas vem eager numa consulta so (a colecao
 *    que mais pesaria em N+1); fontesRenda e abastecimentoAgua carregam lazy
 *    dentro da mesma transacao do service, sem produto cartesiano;
 *  - nada fora do DTO vaza no corpo — em especial senha_hash.
 *
 * Dados ficticios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FamiliaDetalheTest {

    @Autowired MockMvc mvc;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private String bearerAdmin;
    private Familia familia;

    @BeforeEach
    void preparar() {
        familias.deleteAll();
        comunidades.deleteAll();
        municipios.deleteAll();
        usuarios.deleteAll();

        Usuario admin = new Usuario();
        admin.setNome("Admin de Teste");
        admin.setEmail("admin@teste.local");
        admin.setSenhaHash("hash-qualquer");
        admin.setPapel(Papel.ADMIN);
        admin = usuarios.save(admin);
        bearerAdmin = "Bearer " + jwt.gerarAcesso(admin.getId(), admin.getEmail(), admin.getPapel());

        Municipio municipio = municipios.save(
            Municipio.builder().nome("Município de Teste").uf("PE").build());
        Comunidade comunidade = comunidades.save(
            Comunidade.builder().municipio(municipio).nome("Sítio de Teste").build());

        familia = Familia.builder()
            .comunidade(comunidade)
            .responsavelNome("Responsável de Teste")
            .responsavelCpf("00000000000")
            .telefone("87999990000")
            .temBanheiro(true)
            .escoamentoSanitario(EscoamentoSanitario.FOSSA_RUDIMENTAR)
            .tratamentoAgua(TratamentoAgua.SEM_TRATAMENTO)
            .build();
        familia.getAbastecimentoAgua().add(AbastecimentoAgua.CISTERNA);

        // uma pessoa de cada faixa etaria, mais uma sem idade nenhuma
        familia.adicionarPessoa(pessoa("Criança de Teste", LocalDate.now().minusYears(8), true));
        familia.adicionarPessoa(pessoa("Adulta de Teste", LocalDate.now().minusYears(35), false));
        familia.adicionarPessoa(pessoa("Idoso de Teste", LocalDate.now().minusYears(72), false));
        familia.adicionarPessoa(pessoa("Sem Idade de Teste", null, null));

        familia.adicionarFonteRenda(fonte(TipoFonteRenda.APOSENTADORIA));
        familia.adicionarFonteRenda(fonte(TipoFonteRenda.BOLSA_FAMILIA));

        familia = familias.save(familia);
    }

    private static Pessoa pessoa(String nome, LocalDate nascimento, Boolean estuda) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(nome);
        pessoa.setSexo(Sexo.FEMININO);
        pessoa.setDataNascimento(nascimento);
        pessoa.setEstuda(estuda);
        pessoa.setCadastroIncompleto(nascimento == null);
        return pessoa;
    }

    private static FonteRenda fonte(TipoFonteRenda tipo) {
        FonteRenda fonte = new FonteRenda();
        fonte.setTipo(tipo);
        fonte.setFaixa(FaixaRenda.ATE_1_SALARIO);
        return fonte;
    }

    @Test
    @DisplayName("devolve a ficha completa, com comunidade, município, membros e fontes de renda")
    void devolveFichaCompleta() throws Exception {
        mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responsavelNome").value("Responsável de Teste"))
            .andExpect(jsonPath("$.comunidade.nome").value("Sítio de Teste"))
            .andExpect(jsonPath("$.comunidade.municipioNome").value("Município de Teste"))
            .andExpect(jsonPath("$.abastecimentoAgua", hasSize(1)))
            .andExpect(jsonPath("$.pessoas", hasSize(4)))
            .andExpect(jsonPath("$.fontesRenda", hasSize(2)));
    }

    @Test
    @DisplayName("os totais são contados na hora, por faixa etária")
    void contaOsTotais() throws Exception {
        mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totais.totalPessoas").value(4))
            .andExpect(jsonPath("$.totais.totalAte12Anos").value(1))
            .andExpect(jsonPath("$.totais.totalDe13A59Anos").value(1))
            .andExpect(jsonPath("$.totais.total60AnosOuMais").value(1))
            .andExpect(jsonPath("$.totais.totalSemIdadeConhecida").value(1))
            .andExpect(jsonPath("$.totais.totalPessoasEstudando").value(1))
            .andExpect(jsonPath("$.totais.totalFontesRenda").value(2));
    }

    @Test
    @DisplayName("id que não existe é 404 com mensagem em português")
    void idInexistenteE404() throws Exception {
        mvc.perform(get("/api/familias/" + UUID.randomUUID()).header("Authorization", bearerAdmin))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", containsString("não encontrada")));
    }

    @Test
    @DisplayName("comunidade, município e pessoas vêm eager, sem N+1")
    void carregaComunidadeMunicipioEPessoasEager() {
        Familia carregada = familias.buscarDetalhePorId(familia.getId()).orElseThrow();

        assertTrue(Hibernate.isInitialized(carregada.getComunidade()), "comunidade");
        assertTrue(Hibernate.isInitialized(carregada.getComunidade().getMunicipio()), "município");
        assertTrue(Hibernate.isInitialized(carregada.getPessoas()), "pessoas");
        assertEquals(4, carregada.getPessoas().size());
    }

    @Test
    @DisplayName("não devolve senha_hash nem nada fora do DTO")
    void naoVazaCampoForaDoDto() throws Exception {
        mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("senha"))))
            .andExpect(content().string(not(containsString("hash"))))
            .andExpect(jsonPath("$.pessoas[0].familia").doesNotExist())
            .andExpect(jsonPath("$.fontesRenda[0].familia").doesNotExist())
            .andExpect(jsonPath("$.fontesRenda[0].pessoaId").value(nullValue()));
    }

    @Test
    @DisplayName("sem token não passa")
    void semTokenNaoPassa() throws Exception {
        mvc.perform(get("/api/familias/" + familia.getId()))
            .andExpect(status().isUnauthorized());
    }
}
