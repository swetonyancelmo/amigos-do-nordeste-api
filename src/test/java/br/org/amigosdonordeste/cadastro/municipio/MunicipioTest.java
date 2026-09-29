package br.org.amigosdonordeste.cadastro.municipio;

import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de /api/municipios (task 11).
 *
 * Dados ficticios — nenhum municipio real de familia cadastrada entra aqui.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MunicipioTest {

    @Autowired MockMvc mvc;
    @Autowired MunicipioRepositorio municipios;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired FamiliaRepositorio familias;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private String bearer;

    @BeforeEach
    void preparar() {
        // o banco de teste e compartilhado entre as classes: limpa quem
        // referencia municipio antes, senao a FK barra o deleteAll.
        preCadastros.deleteAll();
        familias.deleteAll();
        comunidades.deleteAll();
        municipios.deleteAll();
        usuarios.deleteAll();

        Usuario admin = new Usuario();
        admin.setNome("Admin de Teste");
        admin.setEmail("admin.municipio@teste.local");
        admin.setSenhaHash("hash-qualquer");
        admin.setPapel(Papel.ADMIN);
        admin = usuarios.save(admin);
        bearer = "Bearer " + jwt.gerarAcesso(admin.getId(), admin.getEmail(), admin.getPapel());
    }

    @Test
    @DisplayName("sem token, a rota nao responde")
    void semToken() throws Exception {
        mvc.perform(get("/api/municipios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("lista em ordem alfabetica")
    void listaOrdenada() throws Exception {
        municipios.save(Municipio.builder().nome("Zeta").uf("PE").build());
        municipios.save(Municipio.builder().nome("Alfa").uf("PE").build());

        mvc.perform(get("/api/municipios").header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Alfa"))
            .andExpect(jsonPath("$[1].nome").value("Zeta"));
    }

    @Test
    @DisplayName("cria e devolve 201 com o id")
    void cria() throws Exception {
        mvc.perform(post("/api/municipios")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"  Arcoverde  ","uf":"pe","codigoIbge":"2601102"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.nome").value("Arcoverde"))
            .andExpect(jsonPath("$.uf").value("PE"))
            .andExpect(jsonPath("$.codigoIbge").value("2601102"));
    }

    @Test
    @DisplayName("codigo do IBGE vazio vira null, nao string vazia")
    void codigoVazioViraNull() throws Exception {
        mvc.perform(post("/api/municipios")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Sem Codigo","uf":"PE","codigoIbge":""}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigoIbge").doesNotExist());
    }

    @Test
    @DisplayName("codigo do IBGE repetido e 409")
    void codigoDuplicado() throws Exception {
        municipios.save(Municipio.builder().nome("Primeiro").uf("PE").codigoIbge("2601102").build());

        mvc.perform(post("/api/municipios")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Segundo","uf":"PE","codigoIbge":"2601102"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("UF fora do formato e 400")
    void ufInvalida() throws Exception {
        mvc.perform(post("/api/municipios")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Qualquer","uf":"Pernambuco"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("editar mantendo o proprio codigo do IBGE nao da 409")
    void editaMantendoProprioCodigo() throws Exception {
        Municipio m = municipios.save(
            Municipio.builder().nome("Antigo").uf("PE").codigoIbge("2601102").build());

        mvc.perform(put("/api/municipios/" + m.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Novo","uf":"PE","codigoIbge":"2601102"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Novo"));
    }

    @Test
    @DisplayName("editar com codigo de outro municipio e 409")
    void editaComCodigoDeOutro() throws Exception {
        municipios.save(Municipio.builder().nome("Dono").uf("PE").codigoIbge("2601102").build());
        Municipio outro = municipios.save(Municipio.builder().nome("Outro").uf("PE").build());

        mvc.perform(put("/api/municipios/" + outro.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Outro","uf":"PE","codigoIbge":"2601102"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("editar municipio que nao existe e 404")
    void editaInexistente() throws Exception {
        mvc.perform(put("/api/municipios/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Fantasma","uf":"PE"}
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("buscar por id devolve a ficha; id desconhecido e 404")
    void buscaPorId() throws Exception {
        Municipio m = municipios.save(Municipio.builder().nome("Buscado").uf("PE").build());

        mvc.perform(get("/api/municipios/" + m.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Buscado"));

        mvc.perform(get("/api/municipios/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isNotFound());
    }
}
