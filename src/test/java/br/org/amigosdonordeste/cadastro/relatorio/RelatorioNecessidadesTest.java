package br.org.amigosdonordeste.cadastro.relatorio;

import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #18: "GET /api/relatorios/necessidades — a lista de compras".
 *
 * Os números abaixo foram contados à mão sobre os dados fictícios montados em
 * {@link #preparar()}, para servir de conferência ao critério de aceite "os
 * números batem com uma contagem manual".
 *
 * Dados fictícios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelatorioNecessidadesTest {

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

        // Sítio A1: duas crianças (5 e 10 anos) e uma adulta (30 anos)
        Familia familiaA1 = Familia.builder().comunidade(comunidadeA1).responsavelNome("Responsável A1").build();
        familiaA1.adicionarPessoa(pessoa(5, TamanhoRoupa.INFANTIL_4, "20/21"));
        familiaA1.adicionarPessoa(pessoa(10, TamanhoRoupa.INFANTIL_4, "24/25"));
        familiaA1.adicionarPessoa(pessoa(30, TamanhoRoupa.ADULTO_M, "36/37"));
        familias.save(familiaA1);

        // Sítio A2: uma criança (8 anos, sem calçado cadastrado) e um adulto (40 anos)
        Familia familiaA2 = Familia.builder().comunidade(comunidadeA2).responsavelNome("Responsável A2").build();
        familiaA2.adicionarPessoa(pessoa(8, TamanhoRoupa.INFANTIL_8, null));
        familiaA2.adicionarPessoa(pessoa(40, TamanhoRoupa.ADULTO_G, "38/39"));
        familias.save(familiaA2);

        // Sítio B1 (outro município): fronteira dos 12 anos, um de 13, uma criança
        // sem tamanho de roupa (veio do app da ACS) e um sem idade nenhuma
        Familia familiaB1 = Familia.builder().comunidade(comunidadeB1).responsavelNome("Responsável B1").build();
        familiaB1.adicionarPessoa(pessoa(12, TamanhoRoupa.ADULTO_PP, "28/29"));
        familiaB1.adicionarPessoa(pessoa(13, TamanhoRoupa.ADULTO_P, "30/31"));
        familiaB1.adicionarPessoa(pessoa(7, null, "26/27"));
        familiaB1.adicionarPessoa(pessoaSemIdade(TamanhoRoupa.ADULTO_M));
        familias.save(familiaB1);
    }

    private static Pessoa pessoa(int idade, TamanhoRoupa tamanho, String numeroCalcado) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Pessoa de " + idade + " anos");
        pessoa.setDataNascimento(LocalDate.now().minusYears(idade));
        pessoa.setTamanhoRoupa(tamanho);
        pessoa.setNumeroCalcado(numeroCalcado);
        return pessoa;
    }

    private static Pessoa pessoaSemIdade(TamanhoRoupa tamanho) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Pessoa sem idade");
        pessoa.setCadastroIncompleto(true);
        pessoa.setTamanhoRoupa(tamanho);
        return pessoa;
    }

    @Test
    @DisplayName("sem filtro, soma todas as comunidades e conta só até 12 anos por padrão")
    void semFiltroContaSoAte12Anos() throws Exception {
        mvc.perform(get("/api/relatorios/necessidades").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(3))
            .andExpect(jsonPath("$.totalPessoas").value(9))
            .andExpect(jsonPath("$.totalCriancasAte12").value(5))
            // ordem do enum (Infantil antes de Adulto), não alfabética:
            // alfabeticamente "ADULTO_PP" viria antes de "INFANTIL_4"
            .andExpect(jsonPath("$.roupa", hasSize(3)))
            .andExpect(jsonPath("$.roupa[0].chave").value("INFANTIL_4"))
            .andExpect(jsonPath("$.roupa[0].quantidade").value(2))
            .andExpect(jsonPath("$.roupa[1].chave").value("INFANTIL_8"))
            .andExpect(jsonPath("$.roupa[1].quantidade").value(1))
            .andExpect(jsonPath("$.roupa[2].chave").value("ADULTO_PP"))
            .andExpect(jsonPath("$.roupa[2].quantidade").value(1))
            .andExpect(jsonPath("$.calcado", hasSize(4)))
            .andExpect(jsonPath("$.calcado[0].chave").value("20/21"))
            .andExpect(jsonPath("$.calcado[1].chave").value("24/25"))
            .andExpect(jsonPath("$.calcado[2].chave").value("26/27"))
            .andExpect(jsonPath("$.calcado[3].chave").value("28/29"))
            // a criança de 7 anos sem tamanho não entra em faixa nenhuma (roupa
            // continua com 3 faixas somando 4); a de 8 anos está sem calçado
            .andExpect(jsonPath("$.semTamanhoInformado").value(1))
            .andExpect(jsonPath("$.semCalcadoInformado").value(1))
            // a pessoa sem idade fica fora da roupa/calçado, mas aparece aqui
            .andExpect(jsonPath("$.semIdadeInformada").value(1));
    }

    @Test
    @DisplayName("todasIdades=true conta todo mundo na roupa e no calçado, sem mudar totalPessoas")
    void todasIdadesContaTodoMundo() throws Exception {
        mvc.perform(get("/api/relatorios/necessidades").param("todasIdades", "true")
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPessoas").value(9))
            .andExpect(jsonPath("$.totalCriancasAte12").value(5))
            .andExpect(jsonPath("$.roupa", hasSize(6)))
            .andExpect(jsonPath("$.roupa[0].chave").value("INFANTIL_4"))
            .andExpect(jsonPath("$.roupa[0].quantidade").value(2))
            .andExpect(jsonPath("$.roupa[1].chave").value("INFANTIL_8"))
            .andExpect(jsonPath("$.roupa[2].chave").value("ADULTO_PP"))
            .andExpect(jsonPath("$.roupa[3].chave").value("ADULTO_P"))
            .andExpect(jsonPath("$.roupa[4].chave").value("ADULTO_M"))
            .andExpect(jsonPath("$.roupa[4].quantidade").value(2))
            .andExpect(jsonPath("$.roupa[5].chave").value("ADULTO_G"))
            .andExpect(jsonPath("$.calcado", hasSize(7)))
            .andExpect(jsonPath("$.semTamanhoInformado").value(1))
            // agora a pessoa sem idade também entra, e ela não tem calçado
            .andExpect(jsonPath("$.semCalcadoInformado").value(2))
            .andExpect(jsonPath("$.semIdadeInformada").value(1));
    }

    @Test
    @DisplayName("filtra por comunidadeId")
    void filtraPorComunidade() throws Exception {
        mvc.perform(get("/api/relatorios/necessidades").param("comunidadeId", comunidadeA1Id.toString())
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(1))
            .andExpect(jsonPath("$.totalPessoas").value(3))
            .andExpect(jsonPath("$.totalCriancasAte12").value(2))
            .andExpect(jsonPath("$.roupa", hasSize(1)))
            .andExpect(jsonPath("$.roupa[0].chave").value("INFANTIL_4"))
            .andExpect(jsonPath("$.roupa[0].quantidade").value(2))
            // zero também sai no JSON
            .andExpect(jsonPath("$.semTamanhoInformado").value(0))
            .andExpect(jsonPath("$.semCalcadoInformado").value(0))
            .andExpect(jsonPath("$.semIdadeInformada").value(0));
    }

    @Test
    @DisplayName("filtra por municipioId, somando as comunidades dele")
    void filtraPorMunicipio() throws Exception {
        mvc.perform(get("/api/relatorios/necessidades").param("municipioId", municipioAId.toString())
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(2))
            .andExpect(jsonPath("$.totalPessoas").value(5))
            .andExpect(jsonPath("$.totalCriancasAte12").value(3))
            .andExpect(jsonPath("$.roupa", hasSize(2)))
            .andExpect(jsonPath("$.roupa[0].chave").value("INFANTIL_4"))
            .andExpect(jsonPath("$.roupa[0].quantidade").value(2))
            .andExpect(jsonPath("$.roupa[1].chave").value("INFANTIL_8"))
            .andExpect(jsonPath("$.semTamanhoInformado").value(0))
            .andExpect(jsonPath("$.semCalcadoInformado").value(1))
            .andExpect(jsonPath("$.semIdadeInformada").value(0));
    }

    @Test
    @DisplayName("comunidade sem família nenhuma devolve tudo zerado, não erro")
    void comunidadeSemFamiliaDevolveZerado() throws Exception {
        mvc.perform(get("/api/relatorios/necessidades").param("comunidadeId", UUID.randomUUID().toString())
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(0))
            .andExpect(jsonPath("$.totalPessoas").value(0))
            .andExpect(jsonPath("$.totalCriancasAte12").value(0))
            .andExpect(jsonPath("$.roupa", hasSize(0)))
            .andExpect(jsonPath("$.calcado", hasSize(0)))
            .andExpect(jsonPath("$.semTamanhoInformado").value(0))
            .andExpect(jsonPath("$.semCalcadoInformado").value(0))
            .andExpect(jsonPath("$.semIdadeInformada").value(0));
    }

    @Test
    @DisplayName("sem token não passa")
    void semTokenNaoPassa() throws Exception {
        mvc.perform(get("/api/relatorios/necessidades"))
            .andExpect(status().isUnauthorized());
    }
}
