package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.agente.TokenAgenteService;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/comunidades/opcoes: a lista que o app da agente guarda no aparelho
 * para escolher a comunidade offline. O que importa aqui e o que NAO vem —
 * lider, telefone do lider e coordenadas nao podem ir para o celular.
 *
 * Quem pode chamar a rota esta em SegurancaPapeisTest.
 *
 * Dados ficticios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComunidadeOpcoesTest {

    @Autowired MockMvc mvc;
    @Autowired AgenteRepositorio agentes;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired TokenAgenteService tokens;

    private String bearer;
    private Municipio municipio;

    @BeforeEach
    void preparar() {
        // outra classe de teste pode ter deixado familia no H2 compartilhado,
        // e familia referencia comunidade: sem isto a FK barra o deleteAll
        preCadastros.deleteAll();
        familias.deleteAll();
        agentes.deleteAll();
        comunidades.deleteAll();
        municipios.deleteAll();

        String token = tokens.gerar();
        Agente agente = new Agente();
        agente.setNome("Agente de Teste");
        agente.setTokenHash(TokenAgenteService.hash(token));
        agentes.save(agente);
        bearer = "Bearer " + token;

        municipio = municipios.save(Municipio.builder().nome("Município de Teste").uf("PE").build());
        comunidades.save(Comunidade.builder()
            .municipio(municipio)
            .nome("Sítio B de Teste")
            .liderNome("Líder de Teste")
            .liderTelefone("87900000000")
            .build());
        comunidades.save(Comunidade.builder().municipio(municipio).nome("Sítio A de Teste").build());
    }

    @Test
    @DisplayName("devolve id, nome e município de cada comunidade, em ordem de nome")
    void devolveComunidadeComMunicipio() throws Exception {
        mvc.perform(get("/api/comunidades/opcoes").header("Authorization", bearer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].nome").value("Sítio A de Teste"))
            .andExpect(jsonPath("$[1].nome").value("Sítio B de Teste"))
            .andExpect(jsonPath("$[1].id").isNotEmpty())
            .andExpect(jsonPath("$[1].municipioId").value(municipio.getId().toString()))
            .andExpect(jsonPath("$[1].municipioNome").value("Município de Teste"));
    }

    @Test
    @DisplayName("não manda para o aparelho líder, telefone nem coordenadas")
    void naoMandaDadosDoLider() throws Exception {
        mvc.perform(get("/api/comunidades/opcoes").header("Authorization", bearer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].liderNome").doesNotExist())
            .andExpect(jsonPath("$[1].liderTelefone").doesNotExist())
            .andExpect(jsonPath("$[1].latitude").doesNotExist())
            .andExpect(jsonPath("$[1].longitude").doesNotExist())
            .andExpect(jsonPath("$[1].observaces").doesNotExist());
    }
}
