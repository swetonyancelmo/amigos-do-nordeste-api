package br.org.amigosdonordeste.cadastro.relatorio;

import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #21: "GET /api/relatorios/necessidades.xlsx — backup em Excel".
 *
 * Mesma base de dados fictícia (nenhum nome real) de
 * {@link RelatorioNecessidadesTest}, só que aqui conferimos o arquivo que sai
 * — cabeçalhos, abas e tradução dos enums para o rótulo em português — em vez
 * do JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelatorioNecessidadesXlsxTest {

    @Autowired MockMvc mvc;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private String bearerAdmin;
    private UUID comunidadeId;

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

        Municipio municipio = municipios.save(Municipio.builder().nome("Município Teste").uf("PE").build());
        Comunidade comunidade = comunidades.save(
                Comunidade.builder().municipio(municipio).nome("Sítio Alegre").build());
        comunidadeId = comunidade.getId();

        Familia familia = Familia.builder().comunidade(comunidade).responsavelNome("Responsável Teste").build();
        familia.adicionarPessoa(pessoa("Criança Um", 5, Sexo.FEMININO, Parentesco.FILHO,
                TamanhoRoupa.INFANTIL_4, "20/21"));
        familia.adicionarPessoa(pessoa("Adulta Responsável", 30, Sexo.FEMININO, Parentesco.RESPONSAVEL,
                TamanhoRoupa.ADULTO_M, "36/37"));
        familias.save(familia);
    }

    private static Pessoa pessoa(String nome, int idade, Sexo sexo, Parentesco parentesco,
            TamanhoRoupa tamanho, String numeroCalcado) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(nome);
        pessoa.setDataNascimento(LocalDate.now().minusYears(idade));
        pessoa.setSexo(sexo);
        pessoa.setParentesco(parentesco);
        pessoa.setTamanhoRoupa(tamanho);
        pessoa.setNumeroCalcado(numeroCalcado);
        return pessoa;
    }

    @Test
    @DisplayName("gera o .xlsx com as três abas e os rótulos em português")
    void geraPlanilhaComTresAbas() throws Exception {
        byte[] conteudo = mvc.perform(get("/api/relatorios/necessidades.xlsx")
                .param("todasIdades", "true")
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
            .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(conteudo))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);

            Sheet necessidades = workbook.getSheet("Necessidades");
            assertThat(necessidades).isNotNull();
            assertThat(necessidades.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Indicador");
            assertThat(necessidades.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Total de famílias");
            assertThat(necessidades.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(1);

            Sheet familiasSheet = workbook.getSheet("Famílias");
            assertThat(familiasSheet).isNotNull();
            Row linhaFamilia = familiasSheet.getRow(1);
            assertThat(linhaFamilia.getCell(0).getStringCellValue()).isEqualTo("Sítio Alegre");
            assertThat(linhaFamilia.getCell(1).getStringCellValue()).isEqualTo("Município Teste");
            assertThat(linhaFamilia.getCell(2).getStringCellValue()).isEqualTo("Responsável Teste");
            assertThat(linhaFamilia.getCell(10).getNumericCellValue()).isEqualTo(2); // total de pessoas

            Sheet pessoasSheet = workbook.getSheet("Pessoas");
            assertThat(pessoasSheet).isNotNull();
            // cabeçalho + 2 pessoas; a coleção de pessoas não tem @OrderBy, então
            // a linha da criança é localizada pelo nome em vez de por posição fixa.
            assertThat(pessoasSheet.getLastRowNum()).isEqualTo(2);
            Row linhaCrianca = linhaOndeNomeE(pessoasSheet, "Criança Um");
            assertThat(linhaCrianca.getCell(3).getStringCellValue()).isEqualTo("Feminino"); // rótulo, não FEMININO
            assertThat(linhaCrianca.getCell(7).getStringCellValue()).isEqualTo("Filho");
            assertThat(linhaCrianca.getCell(10).getStringCellValue()).isEqualTo("Infantil 4");
        }
    }

    private static Row linhaOndeNomeE(Sheet sheet, String nome) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (nome.equals(row.getCell(2).getStringCellValue())) {
                return row;
            }
        }
        throw new AssertionError("Nenhuma linha encontrada com nome '" + nome + "'");
    }

    @Test
    @DisplayName("filtrando por comunidade, o nome do arquivo inclui o nome dela")
    void nomeArquivoIncluiComunidade() throws Exception {
        // "í" sai percent-encoded pelo RFC 5987 (filename*=UTF-8''...), então
        // conferimos o trecho sem acento e o marcador de charset UTF-8.
        mvc.perform(get("/api/relatorios/necessidades.xlsx")
                .param("comunidadeId", comunidadeId.toString())
                .header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                    org.hamcrest.Matchers.containsString("filename*=UTF-8''")))
            .andExpect(header().string("Content-Disposition",
                    org.hamcrest.Matchers.containsString("tio-Alegre")));
    }

    @Test
    @DisplayName("sem token não passa")
    void semTokenNaoPassa() throws Exception {
        mvc.perform(get("/api/relatorios/necessidades.xlsx"))
            .andExpect(status().isUnauthorized());
    }
}
