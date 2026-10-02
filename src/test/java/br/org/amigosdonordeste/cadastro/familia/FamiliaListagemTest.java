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
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #16: GET /api/familias com busca, filtros, paginação e totais.
 *
 * Dados fictícios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FamiliaListagemTest {

    @Autowired MockMvc mvc;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;
    @Autowired EntityManagerFactory emf;

    private String bearerAdmin;
    private Municipio municipioA;
    private Municipio municipioB;
    private Comunidade comunidadeA;
    private Comunidade comunidadeB;

    @BeforeEach
    void preparar() {
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

        municipioA = municipios.save(Municipio.builder().nome("Município A").uf("PE").build());
        municipioB = municipios.save(Municipio.builder().nome("Município B").uf("PE").build());
        comunidadeA = comunidades.save(Comunidade.builder().municipio(municipioA).nome("Sítio A").build());
        comunidadeB = comunidades.save(Comunidade.builder().municipio(municipioB).nome("Sítio B").build());
    }

    private Familia salvar(String responsavel, Comunidade comunidade, Boolean temBanheiro, int... idades) {
        Familia familia = Familia.builder()
                .comunidade(comunidade)
                .responsavelNome(responsavel)
                .temBanheiro(temBanheiro)
                .build();
        for (int idade : idades) {
            Pessoa pessoa = new Pessoa();
            pessoa.setNome("Membro de " + responsavel);
            pessoa.setSexo(Sexo.FEMININO);
            pessoa.setDataNascimento(LocalDate.now().minusYears(idade));
            pessoa.setCadastroIncompleto(false);
            familia.adicionarPessoa(pessoa);
        }
        return familias.save(familia);
    }

    @Test
    @DisplayName("busca com e sem acento encontra o mesmo registro")
    void buscaIgnoraAcento() throws Exception {
        Familia jose = salvar("José Teste Silva", comunidadeA, true);
        salvar("Maria Teste", comunidadeA, true);

        for (String termo : new String[] {"Jose", "josé", "JOSE teste"}) {
            mvc.perform(get("/api/familias").param("busca", termo).header("Authorization", bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].id").value(jose.getId().toString()));
        }

        // e o contrário: acento na busca, nome gravado sem
        salvar("Joao Sem Acento", comunidadeA, true);
        mvc.perform(get("/api/familias").param("busca", "João").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(1)))
            .andExpect(jsonPath("$.itens[0].responsavelNome").value("Joao Sem Acento"));
    }

    @Test
    @DisplayName("% e _ na busca são texto, não curinga")
    void buscaNaoInterpretaCuringa() throws Exception {
        salvar("Ana Teste", comunidadeA, true);

        mvc.perform(get("/api/familias").param("busca", "%").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(0)));
        mvc.perform(get("/api/familias").param("busca", "A_a").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(0)));
    }

    @Test
    @DisplayName("filtros são opcionais e combináveis")
    void filtrosCombinaveis() throws Exception {
        Familia aSem = salvar("Ana Sem Banheiro", comunidadeA, false);
        Familia aNaoInformado = salvar("Bia Não Informado", comunidadeA, null);
        salvar("Carla Com Banheiro", comunidadeA, true);
        Familia bSem = salvar("Dora Sem Banheiro", comunidadeB, false);

        mvc.perform(get("/api/familias").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(4)))
            .andExpect(jsonPath("$.total").value(4));

        mvc.perform(get("/api/familias").param("municipioId", municipioB.getId().toString())
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(1)))
            .andExpect(jsonPath("$.itens[0].id").value(bSem.getId().toString()));

        mvc.perform(get("/api/familias").param("comunidadeId", comunidadeA.getId().toString())
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(3)));

        // sem banheiro segue o indicador do dashboard: não informado entra
        mvc.perform(get("/api/familias")
                .param("comunidadeId", comunidadeA.getId().toString())
                .param("semBanheiro", "true")
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(2)))
            .andExpect(jsonPath("$.itens[0].id").value(aSem.getId().toString()))
            .andExpect(jsonPath("$.itens[1].id").value(aNaoInformado.getId().toString()))
            .andExpect(jsonPath("$.itens[1].semBanheiro").value(true));

        mvc.perform(get("/api/familias")
                .param("busca", "sem banheiro")
                .param("municipioId", municipioA.getId().toString())
                .param("comunidadeId", comunidadeA.getId().toString())
                .param("semBanheiro", "true")
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(1)))
            .andExpect(jsonPath("$.itens[0].id").value(aSem.getId().toString()));

        // comunidade de um município e o outro município: nada
        mvc.perform(get("/api/familias")
                .param("municipioId", municipioB.getId().toString())
                .param("comunidadeId", comunidadeA.getId().toString())
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(0)))
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("cada linha traz os totais por faixa etária, calculados")
    void totaisPorFaixa() throws Exception {
        Familia familia = salvar("Ana Totais", comunidadeA, true, 5, 12, 13, 59, 60, 80);
        Pessoa semIdade = new Pessoa();
        semIdade.setNome("Sem Idade");
        semIdade.setSexo(Sexo.MASCULINO);
        semIdade.setCadastroIncompleto(true);
        familia.adicionarPessoa(semIdade);
        familias.save(familia);

        mvc.perform(get("/api/familias").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens[0].totalPessoas").value(7))
            .andExpect(jsonPath("$.itens[0].totalAte12Anos").value(2))
            .andExpect(jsonPath("$.itens[0].totalDe13A59Anos").value(2))
            .andExpect(jsonPath("$.itens[0].total60AnosOuMais").value(2))
            .andExpect(jsonPath("$.itens[0].totalSemIdadeConhecida").value(1))
            .andExpect(jsonPath("$.itens[0].comunidadeNome").value("Sítio A"))
            .andExpect(jsonPath("$.itens[0].municipioNome").value("Município A"));
    }

    @Test
    @DisplayName("paginação: padrão 25, teto 100, e o total não se perde depois da última página")
    void paginacao() throws Exception {
        for (int i = 0; i < 30; i++) {
            salvar(String.format("Responsável %02d", i), comunidadeA, true);
        }

        mvc.perform(get("/api/familias").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(25)))
            .andExpect(jsonPath("$.porPagina").value(25))
            .andExpect(jsonPath("$.total").value(30))
            .andExpect(jsonPath("$.totalPaginas").value(2))
            .andExpect(jsonPath("$.itens[0].responsavelNome").value("Responsável 00"));

        mvc.perform(get("/api/familias").param("pagina", "1").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(5)))
            .andExpect(jsonPath("$.pagina").value(1))
            .andExpect(jsonPath("$.itens[0].responsavelNome").value("Responsável 25"));

        mvc.perform(get("/api/familias").param("porPagina", "1000").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.porPagina").value(100))
            .andExpect(jsonPath("$.itens", hasSize(30)));

        mvc.perform(get("/api/familias").param("pagina", "9").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(0)))
            .andExpect(jsonPath("$.total").value(30));
    }

    @Test
    @DisplayName("uma página de 25 famílias com membros não passa de 3 consultas")
    void semNMaisUm() throws Exception {
        for (int i = 0; i < 30; i++) {
            salvar(String.format("Responsável %02d", i), i % 2 == 0 ? comunidadeA : comunidadeB, true, 5, 40, 70);
        }

        Statistics estatisticas = emf.unwrap(SessionFactory.class).getStatistics();
        estatisticas.setStatisticsEnabled(true);
        estatisticas.clear();
        try {
            mvc.perform(get("/api/familias").header("Authorization", bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(25)))
                .andExpect(jsonPath("$.itens[0].totalPessoas").value(3));

            // a autenticação por JWT não consulta o banco; tudo aqui é da listagem
            long consultas = estatisticas.getPrepareStatementCount();
            // > 0 garante que a estatística está mesmo contando
            assertTrue(consultas > 0 && consultas <= 3, "esperava de 1 a 3 consultas, foram " + consultas);
        } finally {
            estatisticas.setStatisticsEnabled(false);
        }
    }

    @Test
    @DisplayName("sem token é 401")
    void semToken() throws Exception {
        mvc.perform(get("/api/familias")).andExpect(status().isUnauthorized());
    }
}
