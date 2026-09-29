package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que estes testes protegem (issue do GET /api/pre-cadastros):
 *  - o filtro por situacao devolve so a situacao pedida;
 *  - o total de pessoas e contado do payload, nunca lido de coluna;
 *  - familia da mesma comunidade com o mesmo telefone e apontada como
 *    possivel duplicata, mesmo com o numero formatado diferente;
 *  - nome igual com acento diferente ("Jose" x "José") tambem e apontado;
 *  - sem parecido nenhum, ou em outra comunidade, possivelDuplicata e null;
 *  - aprovado nao aponta a propria familia como duplicata;
 *  - token de agente nao ve a fila.
 *
 * Dados ficticios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreCadastroListaTest {

    @Autowired MockMvc mvc;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired FamiliaRepositorio familias;
    @Autowired AgenteRepositorio agentes;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private String bearerAdmin;
    private Agente agente;
    private Comunidade comunidade;
    private Comunidade outraComunidade;
    private Familia familiaExistente;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
        familias.deleteAll();
        agentes.deleteAll();
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

        agente = new Agente();
        agente.setNome("Agente de Teste");
        agente.setTokenHash("hash-qualquer");
        agente = agentes.save(agente);

        Municipio municipio = municipios.save(Municipio.builder().nome("Município de Teste").uf("PE").build());
        comunidade = comunidades.save(Comunidade.builder().municipio(municipio).nome("Sítio de Teste").build());
        outraComunidade = comunidades.save(Comunidade.builder().municipio(municipio).nome("Sítio Vizinho").build());

        familiaExistente = familias.save(Familia.builder()
            .comunidade(comunidade)
            .responsavelNome("José de Teste")
            .telefone("+55 (87) 9.9999-0000")
            .build());
    }

    @AfterEach
    void limpar() {
        preCadastros.deleteAll();
        familias.deleteAll();
    }

    private PreCadastro salvar(String responsavelNome, String telefone, Comunidade comunidade,
                               int pessoas, SituacaoPreCadastro situacao) {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < pessoas; i++) {
            if (i > 0) lista.append(',');
            lista.append("""
                { "id": "%s", "nome": "Pessoa %d", "cadastroIncompleto": false }
                """.formatted(UUID.randomUUID(), i));
        }
        PreCadastro p = new PreCadastro();
        p.setId(UUID.randomUUID());
        p.setAgente(agente);
        p.setComunidade(comunidade);
        p.setSituacao(situacao);
        p.setRecebidoEm(OffsetDateTime.now());
        p.setPayload("""
            {
              "id": "%s",
              "responsavelNome": %s,
              "telefone": %s,
              "comunidadeNome": "Sítio Escrito à Mão",
              "criadoEm": "2026-09-12T09:12:00Z",
              "pessoas": [%s]
            }
            """.formatted(p.getId(), json(responsavelNome), json(telefone), lista));
        return preCadastros.save(p);
    }

    private static String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private MockHttpServletRequestBuilder listar(String query) {
        return get("/api/pre-cadastros" + query).header("Authorization", bearerAdmin);
    }

    @Test
    @DisplayName("filtra por situação e conta as pessoas do payload")
    void filtraPorSituacaoEContaPessoas() throws Exception {
        PreCadastro pendente = salvar("Responsável Sem Par", "87911110000", comunidade, 3, SituacaoPreCadastro.PENDENTE);
        salvar("Outra Responsável", "87922220000", comunidade, 1, SituacaoPreCadastro.APROVADO);

        mvc.perform(listar("?situacao=PENDENTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(pendente.getId().toString()))
            .andExpect(jsonPath("$[0].responsavelNome").value("Responsável Sem Par"))
            .andExpect(jsonPath("$[0].comunidadeId").value(comunidade.getId().toString()))
            .andExpect(jsonPath("$[0].comunidadeNome").value("Sítio de Teste"))
            .andExpect(jsonPath("$[0].totalPessoas").value(3))
            .andExpect(jsonPath("$[0].agenteNome").value("Agente de Teste"))
            .andExpect(jsonPath("$[0].recebidoEm").isString())
            .andExpect(jsonPath("$[0].situacao").value("PENDENTE"))
            .andExpect(jsonPath("$[0].possivelDuplicata", nullValue()));

        mvc.perform(listar("?situacao=APROVADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].situacao").value("APROVADO"));

        mvc.perform(listar(""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("mesmo telefone na mesma comunidade é possível duplicata, mesmo formatado diferente")
    void duplicataPorTelefone() throws Exception {
        salvar("Nome Bem Diferente", "5587999990000", comunidade, 2, SituacaoPreCadastro.PENDENTE);

        mvc.perform(listar("?situacao=PENDENTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].possivelDuplicata.familiaId").value(familiaExistente.getId().toString()))
            .andExpect(jsonPath("$[0].possivelDuplicata.nome").value("José de Teste"))
            .andExpect(jsonPath("$[0].possivelDuplicata.motivo").value("TELEFONE_IGUAL"));
    }

    @Test
    @DisplayName("aprovado não aponta a própria família como duplicata")
    void aprovadoNaoApontaAPropriaFamilia() throws Exception {
        PreCadastro aprovado = salvar("José de Teste", "87999990000", comunidade, 1, SituacaoPreCadastro.APROVADO);
        aprovado.setFamilia(familiaExistente);
        preCadastros.save(aprovado);

        mvc.perform(listar("?situacao=APROVADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].possivelDuplicata", nullValue()));
    }

    @Test
    @DisplayName("nome com acento diferente na mesma comunidade é possível duplicata")
    void duplicataPorNomeSemAcento() throws Exception {
        salvar("jose de teste", "87933330000", comunidade, 2, SituacaoPreCadastro.PENDENTE);

        mvc.perform(listar("?situacao=PENDENTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].possivelDuplicata.familiaId").value(familiaExistente.getId().toString()))
            .andExpect(jsonPath("$[0].possivelDuplicata.motivo").value("NOME_PARECIDO"));
    }

    @Test
    @DisplayName("mesmo nome em outra comunidade não é duplicata")
    void mesmoNomeEmOutraComunidadeNaoEDuplicata() throws Exception {
        salvar("Jose de Teste", "87999990000", outraComunidade, 2, SituacaoPreCadastro.PENDENTE);

        mvc.perform(listar("?situacao=PENDENTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].comunidadeNome").value("Sítio Vizinho"))
            .andExpect(jsonPath("$[0].possivelDuplicata", nullValue()));
    }

    @Test
    @DisplayName("sem comunidade reconhecida, mostra o nome escrito pela agente e não procura duplicata")
    void semComunidadeUsaNomeDoPayload() throws Exception {
        salvar("Jose de Teste", "87999990000", null, 1, SituacaoPreCadastro.PENDENTE);

        mvc.perform(listar("?situacao=PENDENTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].comunidadeId", nullValue()))
            .andExpect(jsonPath("$[0].comunidadeNome").value("Sítio Escrito à Mão"))
            .andExpect(jsonPath("$[0].possivelDuplicata", nullValue()));
    }

    @Test
    @DisplayName("situação desconhecida é 400")
    void situacaoDesconhecidaE400() throws Exception {
        mvc.perform(listar("?situacao=QUALQUER")).andExpect(status().isBadRequest());
    }
}
