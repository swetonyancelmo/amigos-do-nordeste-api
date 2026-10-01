package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
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

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de POST /api/comunidades (auditoria de integracao, CONTRATO-04):
 * os nomes da resposta sao os mesmos da requisicao (tipoComunidade,
 * observacoes) e os tamanhos batem com as colunas.
 *
 * Dados ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComunidadeTest {

    @Autowired MockMvc mvc;
    @Autowired MunicipioRepositorio municipios;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired FamiliaRepositorio familias;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired AgenteRepositorio agentes;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private String bearer;
    private Municipio municipio;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
        agentes.deleteAll();
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
        bearer = "Bearer " + jwt.gerarAcesso(admin.getId(), admin.getEmail(), admin.getPapel());

        municipio = municipios.save(Municipio.builder().nome("Município de Teste").uf("PE").build());
    }

    private String corpo(String liderNome, String liderTelefone) {
        return """
            {"nome": "Sítio de Teste", "municipioId": "%s", "tipoComunidade": "POVOADO",
             "liderNome": "%s", "liderTelefone": "%s", "observacoes": "Nota de teste"}
            """.formatted(municipio.getId(), liderNome, liderTelefone);
    }

    @Test
    @DisplayName("a resposta usa tipoComunidade e observacoes, como a requisicao")
    void nomesDaResposta() throws Exception {
        mvc.perform(post("/api/comunidades").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("Líder de Teste", "87999990000")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoComunidade").value("POVOADO"))
            .andExpect(jsonPath("$.observacoes").value("Nota de teste"))
            .andExpect(jsonPath("$.tipo").doesNotExist())
            .andExpect(jsonPath("$.observaces").doesNotExist());
    }

    @Test
    @DisplayName("lider vazio, como o formulario manda, e aceito e gravado como null")
    void liderVazio() throws Exception {
        mvc.perform(post("/api/comunidades").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("", "")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.liderNome").doesNotExist())
            .andExpect(jsonPath("$.liderTelefone").doesNotExist());
    }

    @Test
    @DisplayName("telefone do lider maior que a coluna e recusado pela validacao, com o campo na mensagem")
    void telefoneLongo() throws Exception {
        mvc.perform(post("/api/comunidades").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("Líder de Teste", "8".repeat(21))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", startsWith("liderTelefone")));
    }
}
