package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroRepositorio;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #43: família não se apaga, se inativa.
 *  - inativa some da listagem e dos relatórios (os totais do dashboard);
 *  - incluirInativas=true acha a inativa, e dá para reativar;
 *  - a ficha da inativa continua abrindo, marcada como inativa;
 *  - não entra na detecção de duplicata do pré-cadastro;
 *  - id inexistente é 404.
 *
 * Dados fictícios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FamiliaInativacaoTest {

    @Autowired MockMvc mvc;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private String bearerAdmin;
    private Comunidade comunidade;
    private Familia ativa;
    private Familia aInativar;

    @BeforeEach
    void preparar() {
        // pre_cadastro aprovado referencia familia: limpa primeiro
        preCadastros.deleteAll();
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

        Municipio municipio = municipios.save(Municipio.builder().nome("Município de Teste").uf("PE").build());
        comunidade = comunidades.save(Comunidade.builder().municipio(municipio).nome("Sítio de Teste").build());

        ativa = familias.save(familia("Responsável Ativa", "87999990001"));
        aInativar = familias.save(familia("Responsável Duplicada", "87999990002"));
    }

    private Familia familia(String responsavel, String telefone) {
        Familia familia = Familia.builder()
                .comunidade(comunidade)
                .responsavelNome(responsavel)
                .telefone(telefone)
                .temBanheiro(false)
                .build();
        Pessoa crianca = new Pessoa();
        crianca.setNome("Criança de " + responsavel);
        crianca.setSexo(Sexo.FEMININO);
        crianca.setDataNascimento(LocalDate.now().minusYears(6));
        crianca.setCadastroIncompleto(false);
        familia.adicionarPessoa(crianca);
        return familia;
    }

    private void inativar(UUID id) throws Exception {
        mvc.perform(post("/api/familias/" + id + "/inativar").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.ativa").value(false));
    }

    @Test
    @DisplayName("família nova nasce ativa e aparece na listagem")
    void familiaNovaNasceAtiva() throws Exception {
        mvc.perform(get("/api/familias").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].ativa").value(true))
            .andExpect(jsonPath("$[0].comunidadeNome").value("Sítio de Teste"))
            .andExpect(jsonPath("$[0].municipioNome").value("Município de Teste"));
    }

    @Test
    @DisplayName("inativa some da listagem; com incluirInativas=true volta, marcada")
    void inativaSomeDaListagem() throws Exception {
        inativar(aInativar.getId());

        mvc.perform(get("/api/familias").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(ativa.getId().toString()));

        mvc.perform(get("/api/familias").param("incluirInativas", "true").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[?(@.id == '" + aInativar.getId() + "')].ativa").value(false));
    }

    @Test
    @DisplayName("inativa não entra nos totais do dashboard nem nos relatórios")
    void inativaNaoEntraNosRelatorios() throws Exception {
        inativar(aInativar.getId());

        mvc.perform(get("/api/relatorios/situacao").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(1))
            .andExpect(jsonPath("$.semBanheiro.valor").value(1));

        mvc.perform(get("/api/relatorios/necessidades").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(1))
            .andExpect(jsonPath("$.totalPessoas").value(1))
            .andExpect(jsonPath("$.totalCriancasAte12").value(1));
    }

    @Test
    @DisplayName("reativar devolve a família à listagem e aos totais")
    void reativar() throws Exception {
        inativar(aInativar.getId());

        mvc.perform(post("/api/familias/" + aInativar.getId() + "/reativar").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ativa").value(true));

        mvc.perform(get("/api/familias").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/relatorios/situacao").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.totalFamilias").value(2));
        mvc.perform(get("/api/relatorios/necessidades").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.totalPessoas").value(2));
    }

    @Test
    @DisplayName("inativar duas vezes não é erro, e nada é apagado")
    void inativarDuasVezesNaoApaga() throws Exception {
        inativar(aInativar.getId());
        inativar(aInativar.getId());

        assertTrue(familias.existsById(aInativar.getId()));
        mvc.perform(get("/api/familias/" + aInativar.getId()).header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ativa").value(false))
            .andExpect(jsonPath("$.pessoas", hasSize(1)));
    }

    @Test
    @DisplayName("inativa não é apontada como duplicata do pré-cadastro")
    void inativaNaoEhDuplicata() throws Exception {
        inativar(aInativar.getId());

        assertTrue(familias.findByComunidadeIdAndAtivaTrueOrderByResponsavelNomeAsc(comunidade.getId())
                .stream().noneMatch(f -> f.getId().equals(aInativar.getId())));
        assertTrue(familias.buscarPorNomeParecidoNaComunidade(comunidade.getId(), "Responsável Duplicada")
                .isEmpty());
    }

    @Test
    @DisplayName("id inexistente é 404, nos dois sentidos")
    void idInexistente() throws Exception {
        mvc.perform(post("/api/familias/" + UUID.randomUUID() + "/inativar").header("Authorization", bearerAdmin))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/familias/" + UUID.randomUUID() + "/reativar").header("Authorization", bearerAdmin))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sem token não passa")
    void semTokenNaoPassa() throws Exception {
        mvc.perform(post("/api/familias/" + ativa.getId() + "/inativar"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/familias"))
            .andExpect(status().isUnauthorized());
    }
}
