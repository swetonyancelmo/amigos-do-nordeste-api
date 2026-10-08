package br.org.amigosdonordeste.cadastro.relatorio;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/relatorios/mapa — um ponto por comunidade (ADR-0005). Dados fictícios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelatorioMapaTest {

    @Autowired MockMvc mvc;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private String bearerAdmin;
    private UUID municipioAId;
    private UUID municipioSemIbgeId;

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

        Municipio a = municipios.save(Municipio.builder().nome("Município A").uf("PE").codigoIbge("2600001").build());
        Municipio b = municipios.save(Municipio.builder().nome("Município B").uf("PE").build());
        municipioAId = a.getId();
        municipioSemIbgeId = b.getId();

        Comunidade a1 = comunidades.save(Comunidade.builder().municipio(a).nome("Sítio A1")
                .latitude(new BigDecimal("-8.5390000")).longitude(new BigDecimal("-37.6960000")).build());
        comunidades.save(Comunidade.builder().municipio(a).nome("Sítio A2").build());
        Comunidade b1 = comunidades.save(Comunidade.builder().municipio(b).nome("Sítio B1").build());

        familias.save(familia(a1, "Responsável 1"));
        familias.save(familia(a1, "Responsável 2"));
        Familia inativa = familia(a1, "Responsável 3");
        inativa.inativar();
        familias.save(inativa);
        familias.save(familia(b1, "Responsável 4"));
    }

    private static Familia familia(Comunidade comunidade, String responsavel) {
        return Familia.builder().comunidade(comunidade).responsavelNome(responsavel).build();
    }

    @Test
    @DisplayName("sem filtro: municipio null, todas as comunidades, inativa não conta, sem coordenada fica nula")
    void semFiltro() throws Exception {
        mvc.perform(get("/api/relatorios/mapa").header("Authorization", bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio").value((Object) null))
                .andExpect(jsonPath("$.pontos.length()").value(3))
                .andExpect(jsonPath("$.pontos[0].nome").value("Sítio A1"))
                .andExpect(jsonPath("$.pontos[0].familias").value(2))
                .andExpect(jsonPath("$.pontos[0].latitude").value(-8.539))
                .andExpect(jsonPath("$.pontos[1].nome").value("Sítio A2"))
                .andExpect(jsonPath("$.pontos[1].familias").value(0))
                .andExpect(jsonPath("$.pontos[1].latitude").value((Object) null))
                .andExpect(jsonPath("$.pontos[1].longitude").value((Object) null));
    }

    @Test
    @DisplayName("com municipioId: devolve o município com codigoIbge e só as comunidades dele")
    void comFiltro() throws Exception {
        mvc.perform(get("/api/relatorios/mapa").param("municipioId", municipioAId.toString())
                        .header("Authorization", bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio.id").value(municipioAId.toString()))
                .andExpect(jsonPath("$.municipio.nome").value("Município A"))
                .andExpect(jsonPath("$.municipio.codigoIbge").value("2600001"))
                .andExpect(jsonPath("$.pontos.length()").value(2))
                .andExpect(jsonPath("$.pontos[0].comunidadeId").exists());
    }

    @Test
    @DisplayName("município sem código IBGE devolve codigoIbge null, sem erro")
    void semCodigoIbge() throws Exception {
        mvc.perform(get("/api/relatorios/mapa").param("municipioId", municipioSemIbgeId.toString())
                        .header("Authorization", bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio.codigoIbge").value((Object) null))
                .andExpect(jsonPath("$.pontos.length()").value(1));
    }

    @Test
    @DisplayName("municipioId inexistente é 404 e sem token é 401/403")
    void inexistenteESemToken() throws Exception {
        mvc.perform(get("/api/relatorios/mapa").param("municipioId", UUID.randomUUID().toString())
                        .header("Authorization", bearerAdmin))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/relatorios/mapa")).andExpect(status().is4xxClientError());
    }
}
