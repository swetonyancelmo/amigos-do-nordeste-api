package br.org.amigosdonordeste.cadastro.relatorio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;
import br.org.amigosdonordeste.cadastro.familia.FamiliaDetalheResponse;
import br.org.amigosdonordeste.cadastro.familia.PessoaResponse;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import br.org.amigosdonordeste.cadastro.relatorio.NecessidadesResponse.ItemContagem;

/**
 * Issue #21: monta o .xlsx do relatório de necessidades — três abas
 * (Necessidades, Famílias, Pessoas) a partir dos mesmos dados que já saem em
 * JSON em {@code GET /api/relatorios/necessidades}. Classe utilitária pura
 * (sem estado, sem Spring): só transforma DTO em planilha.
 */
final class RelatorioExcelBuilder {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RelatorioExcelBuilder() {
    }

    static byte[] gerar(NecessidadesResponse necessidades, List<FamiliaDetalheResponse> familias) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle estiloCabecalho = estiloCabecalho(workbook);

            abaNecessidades(workbook, estiloCabecalho, necessidades);
            abaFamilias(workbook, estiloCabecalho, familias);
            abaPessoas(workbook, estiloCabecalho, familias);

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            workbook.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar planilha de necessidades", e);
        }
    }

    private static CellStyle estiloCabecalho(XSSFWorkbook workbook) {
        Font negrito = workbook.createFont();
        negrito.setBold(true);
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(negrito);
        return estilo;
    }

    private static void abaNecessidades(XSSFWorkbook workbook, CellStyle estiloCabecalho,
            NecessidadesResponse necessidades) {
        Sheet sheet = workbook.createSheet("Necessidades");
        int linha = 0;

        linha = cabecalho(sheet, linha, estiloCabecalho, "Indicador", "Valor");
        linha = linha(sheet, linha, "Total de famílias", necessidades.totalFamilias());
        linha = linha(sheet, linha, "Total de pessoas", necessidades.totalPessoas());
        linha = linha(sheet, linha, "Total de crianças até 12 anos", necessidades.totalCriancasAte12());
        linha = linha(sheet, linha, "Sem tamanho de roupa informado", necessidades.semTamanhoInformado());
        linha = linha(sheet, linha, "Sem calçado informado", necessidades.semCalcadoInformado());
        linha = linha(sheet, linha, "Sem idade informada", necessidades.semIdadeInformada());

        linha++;
        linha = cabecalho(sheet, linha, estiloCabecalho, "Tamanho de roupa", "Quantidade");
        for (ItemContagem item : necessidades.roupa()) {
            linha = linha(sheet, linha, TamanhoRoupa.valueOf(item.chave()).getRotulo(), item.quantidade());
        }

        linha++;
        linha = cabecalho(sheet, linha, estiloCabecalho, "Número de calçado", "Quantidade");
        for (ItemContagem item : necessidades.calcado()) {
            linha = linha(sheet, linha, item.chave(), item.quantidade());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private static void abaFamilias(XSSFWorkbook workbook, CellStyle estiloCabecalho,
            List<FamiliaDetalheResponse> familias) {
        Sheet sheet = workbook.createSheet("Famílias");
        String[] colunas = {
                "Comunidade", "Município", "Responsável", "CPF", "Telefone", "Ponto de referência",
                "Tem banheiro", "Escoamento sanitário", "Tratamento de água", "Abastecimento de água",
                "Total de pessoas", "Até 12 anos", "De 13 a 59 anos", "60 anos ou mais",
                "Pessoas estudando", "Fontes de renda", "Observações"
        };
        int linha = cabecalho(sheet, 0, estiloCabecalho, colunas);

        for (FamiliaDetalheResponse familia : familias) {
            Row row = sheet.createRow(linha++);
            int col = 0;
            escrever(row, col++, familia.comunidade().nome());
            escrever(row, col++, familia.comunidade().municipioNome());
            escrever(row, col++, familia.responsavelNome());
            escrever(row, col++, familia.responsavelCpf());
            escrever(row, col++, familia.telefone());
            escrever(row, col++, familia.pontoReferencia());
            escrever(row, col++, familia.temBanheiro());
            escrever(row, col++, familia.escoamentoSanitario());
            escrever(row, col++, familia.tratamentoAgua());
            escrever(row, col++, rotulos(familia.abastecimentoAgua()));
            escrever(row, col++, familia.totais().totalPessoas());
            escrever(row, col++, familia.totais().totalAte12Anos());
            escrever(row, col++, familia.totais().totalDe13A59Anos());
            escrever(row, col++, familia.totais().total60AnosOuMais());
            escrever(row, col++, familia.totais().totalPessoasEstudando());
            escrever(row, col++, familia.totais().totalFontesRenda());
            escrever(row, col, familia.observacoes());
        }

        for (int col = 0; col < colunas.length; col++) {
            sheet.autoSizeColumn(col);
        }
    }

    private static void abaPessoas(XSSFWorkbook workbook, CellStyle estiloCabecalho,
            List<FamiliaDetalheResponse> familias) {
        Sheet sheet = workbook.createSheet("Pessoas");
        String[] colunas = {
                "Comunidade", "Responsável da família", "Nome", "Sexo", "Data de nascimento",
                "Idade estimada", "Idade", "Parentesco", "Estuda", "Série", "Tamanho de roupa",
                "Número de calçado", "Gestante", "Cadastro incompleto", "Observações"
        };
        int linha = cabecalho(sheet, 0, estiloCabecalho, colunas);

        for (FamiliaDetalheResponse familia : familias) {
            for (PessoaResponse pessoa : familia.pessoas()) {
                Row row = sheet.createRow(linha++);
                int col = 0;
                escrever(row, col++, familia.comunidade().nome());
                escrever(row, col++, familia.responsavelNome());
                escrever(row, col++, pessoa.nome());
                escrever(row, col++, pessoa.sexo());
                escrever(row, col++, pessoa.dataNascimento());
                escrever(row, col++, pessoa.idadeEstimada());
                escrever(row, col++, pessoa.idade());
                escrever(row, col++, pessoa.parentesco());
                escrever(row, col++, pessoa.estuda());
                escrever(row, col++, pessoa.serie());
                escrever(row, col++, pessoa.tamanhoRoupa());
                escrever(row, col++, pessoa.numeroCalcado());
                escrever(row, col++, pessoa.gestante());
                escrever(row, col++, pessoa.cadastroIncompleto());
                escrever(row, col, pessoa.observacoes());
            }
        }

        for (int col = 0; col < colunas.length; col++) {
            sheet.autoSizeColumn(col);
        }
    }

    private static String rotulos(Set<? extends Rotulavel> valores) {
        return valores.stream().map(Rotulavel::getRotulo).collect(Collectors.joining(", "));
    }

    private static int cabecalho(Sheet sheet, int linha, CellStyle estilo, String... colunas) {
        Row row = sheet.createRow(linha);
        for (int col = 0; col < colunas.length; col++) {
            Cell cell = row.createCell(col);
            cell.setCellValue(colunas[col]);
            cell.setCellStyle(estilo);
        }
        return linha + 1;
    }

    private static int linha(Sheet sheet, int linha, String rotulo, long valor) {
        Row row = sheet.createRow(linha);
        row.createCell(0).setCellValue(rotulo);
        row.createCell(1).setCellValue(valor);
        return linha + 1;
    }

    /** Despacha pelo tipo em vez de sobrecarregar setCellValue — cobre os tipos que aparecem nos DTOs. */
    private static void escrever(Row row, int col, Object valor) {
        Cell cell = row.createCell(col);
        switch (valor) {
            case null -> cell.setBlank();
            case String s -> cell.setCellValue(s);
            case Boolean b -> cell.setCellValue(b ? "Sim" : "Não");
            case Integer i -> cell.setCellValue(i);
            case Long l -> cell.setCellValue(l);
            case LocalDate d -> cell.setCellValue(d.format(DATA_BR));
            case Rotulavel r -> cell.setCellValue(r.getRotulo());
            default -> cell.setCellValue(valor.toString());
        }
    }
}
