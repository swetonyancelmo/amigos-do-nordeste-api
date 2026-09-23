package br.org.amigosdonordeste.cadastro.relatorio;

import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #19: "GET /api/relatorios/situacao — indicadores das famílias".
 *
 * Os números abaixo foram contados à mão sobre os dados fictícios montados em
 * {@link #preparar()}. Dados fictícios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelatorioSituacaoTest {

    @Autowired MockMvc mvc;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private String bearerAdmin;
    private UUID comunidadeA1Id;
    private UUID municipioAId;

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

        Municipio municipioA = municipios.save(Municipio.builder().nome("Município A").uf("PE").build());
        Municipio municipioB = municipios.save(Municipio.builder().nome("Município B").uf("PE").build());
        municipioAId = municipioA.getId();

        Comunidade comunidadeA1 = comunidades.save(Comunidade.builder().municipio(municipioA).nome("Sítio A1").build());
        Comunidade comunidadeA2 = comunidades.save(Comunidade.builder().municipio(municipioA).nome("Sítio A2").build());
        Comunidade comunidadeB1 = comunidades.save(Comunidade.builder().municipio(municipioB).nome("Sítio B1").build());
        comunidadeA1Id = comunidadeA1.getId();

        // A1: tem banheiro, só carro-pipa, só Bolsa Família, sem tratamento
        familias.save(familia(comunidadeA1, "Responsável 1", true,
                Set.of(AbastecimentoAgua.CARRO_PIPA),
                List.of(TipoFonteRenda.BOLSA_FAMILIA),
                TratamentoAgua.SEM_TRATAMENTO));

        // A1: sem banheiro; carro-pipa e Bolsa Família, mas não são os únicos
        familias.save(familia(comunidadeA1, "Responsável 2", false,
                Set.of(AbastecimentoAgua.CARRO_PIPA, AbastecimentoAgua.CISTERNA),
                List.of(TipoFonteRenda.BOLSA_FAMILIA, TipoFonteRenda.APOSENTADORIA),
                TratamentoAgua.FERVIDA));

        // A2: banheiro e tratamento não informados, sem abastecimento cadastrado
        familias.save(familia(comunidadeA2, "Responsável 3", null,
                Set.of(),
                List.of(TipoFonteRenda.APOSENTADORIA),
                null));

        // B1 (outro município): sem banheiro, só carro-pipa, sem fonte de renda, sem tratamento
        familias.save(familia(comunidadeB1, "Responsável 4", false,
                Set.of(AbastecimentoAgua.CARRO_PIPA),
                List.of(),
                TratamentoAgua.SEM_TRATAMENTO));
    }

    private static Familia familia(Comunidade comunidade, String responsavel, Boolean temBanheiro,
                                   Set<AbastecimentoAgua> abastecimento, List<TipoFonteRenda> fontes,
                                   TratamentoAgua tratamento) {
        Familia familia = Familia.builder()
                .comunidade(comunidade)
                .responsavelNome(responsavel)
                .temBanheiro(temBanheiro)
                .tratamentoAgua(tratamento)
                .build();
        familia.getAbastecimentoAgua().addAll(abastecimento);
        fontes.forEach(tipo -> familia.adicionarFonteRenda(FonteRenda.builder().tipo(tipo).build()));
        return familia;
    }

    @Test
    @DisplayName("sem filtro, conta todas as famílias e calcula o percentual sobre o total")
    void semFiltro() throws Exception {
        mvc.perform(get("/api/relatorios/situacao").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(4))
            // false e não informado contam como sem banheiro
            .andExpect(jsonPath("$.semBanheiro.valor").value(3))
            .andExpect(jsonPath("$.semBanheiro.percentual").value(75.0))
            // carro-pipa junto com cisterna não é "só carro-pipa"
            .andExpect(jsonPath("$.soCarroPipa.valor").value(2))
            .andExpect(jsonPath("$.soCarroPipa.percentual").value(50.0))
            .andExpect(jsonPath("$.soBolsaFamilia.valor").value(1))
            .andExpect(jsonPath("$.soBolsaFamilia.percentual").value(25.0))
            .andExpect(jsonPath("$.semTratamentoAgua.valor").value(2))
            .andExpect(jsonPath("$.semTratamentoAgua.percentual").value(50.0));
    }

    @Test
    @DisplayName("filtra por comunidadeId")
    void filtraPorComunidade() throws Exception {
        mvc.perform(get("/api/relatorios/situacao").param("comunidadeId", comunidadeA1Id.toString())
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(2))
            .andExpect(jsonPath("$.semBanheiro.valor").value(1))
            .andExpect(jsonPath("$.soCarroPipa.valor").value(1))
            .andExpect(jsonPath("$.soBolsaFamilia.valor").value(1))
            .andExpect(jsonPath("$.semTratamentoAgua.valor").value(1))
            .andExpect(jsonPath("$.semTratamentoAgua.percentual").value(50.0));
    }

    @Test
    @DisplayName("filtra por municipioId, com percentual arredondado em duas casas")
    void filtraPorMunicipio() throws Exception {
        mvc.perform(get("/api/relatorios/situacao").param("municipioId", municipioAId.toString())
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(3))
            .andExpect(jsonPath("$.semBanheiro.valor").value(2))
            .andExpect(jsonPath("$.semBanheiro.percentual").value(66.67))
            .andExpect(jsonPath("$.soCarroPipa.valor").value(1))
            .andExpect(jsonPath("$.soCarroPipa.percentual").value(33.33))
            .andExpect(jsonPath("$.soBolsaFamilia.valor").value(1))
            .andExpect(jsonPath("$.semTratamentoAgua.valor").value(1));
    }

    @Test
    @DisplayName("filtro sem família nenhuma devolve zero, não divide por zero")
    void semFamiliaDevolveZerado() throws Exception {
        mvc.perform(get("/api/relatorios/situacao").param("comunidadeId", UUID.randomUUID().toString())
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(0))
            .andExpect(jsonPath("$.semBanheiro.valor").value(0))
            .andExpect(jsonPath("$.semBanheiro.percentual").value(0.0))
            .andExpect(jsonPath("$.soCarroPipa.percentual").value(0.0))
            .andExpect(jsonPath("$.soBolsaFamilia.percentual").value(0.0))
            .andExpect(jsonPath("$.semTratamentoAgua.percentual").value(0.0));
    }

    @Test
    @DisplayName("sem token não passa")
    void semTokenNaoPassa() throws Exception {
        mvc.perform(get("/api/relatorios/situacao"))
            .andExpect(status().isUnauthorized());
    }
}
