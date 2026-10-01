package br.org.amigosdonordeste.cadastro.metadados;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/metadados e publico e traz toda lista fechada que o web monta em select. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetadadosTest {

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("parentesco esta nos metadados, com valor e rotulo (auditoria, CONTRATO-02)")
    void parentesco() throws Exception {
        mvc.perform(get("/api/metadados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parentesco[*].valor", hasItem("FILHO")))
            .andExpect(jsonPath("$.parentesco[*].valor", hasItem("OUTRO_PARENTE")))
            .andExpect(jsonPath("$.parentesco[?(@.valor == 'CONJUGE')].rotulo", hasItem("Cônjuge")));
    }

    @Test
    @DisplayName("situacao do pre-cadastro esta nos metadados, para o filtro da tela de Chamados")
    void situacaoPreCadastro() throws Exception {
        mvc.perform(get("/api/metadados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacaoPreCadastro[*].valor", hasItem("PENDENTE")))
            .andExpect(jsonPath("$.situacaoPreCadastro[?(@.valor == 'DEVOLVIDO')].rotulo", hasItem("Devolvido")));
    }
}
