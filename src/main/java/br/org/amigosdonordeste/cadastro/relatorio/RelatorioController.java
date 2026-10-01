package br.org.amigosdonordeste.cadastro.relatorio;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "relatorios")
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @Operation(summary = "Necessidades de roupa e calçado por comunidade — a lista de compras (issue #18)")
    @GetMapping("/necessidades")
    public NecessidadesResponse necessidades(
            @Parameter(description = "Filtra por comunidade; omitido, soma todas as comunidades")
            @RequestParam(required = false) UUID comunidadeId,
            @Parameter(description = "Filtra por município (dashboard); combina com comunidadeId se os dois vierem")
            @RequestParam(required = false) UUID municipioId,
            @Parameter(description = "false (padrão) conta só até 12 anos; true conta todo mundo")
            @RequestParam(defaultValue = "false") boolean todasIdades) {
        return relatorioService.necessidades(comunidadeId, municipioId, todasIdades);
    }

    @Operation(summary = "Mesmo relatório de necessidades em .xlsx, com abas de Famílias e Pessoas — backup (issue #21)")
    @GetMapping("/necessidades.xlsx")
    public ResponseEntity<byte[]> necessidadesXlsx(
            @Parameter(description = "Filtra por comunidade; omitido, soma todas as comunidades")
            @RequestParam(required = false) UUID comunidadeId,
            @Parameter(description = "Filtra por município (dashboard); combina com comunidadeId se os dois vierem")
            @RequestParam(required = false) UUID municipioId,
            @Parameter(description = "false (padrão) conta só até 12 anos na aba Necessidades; true conta todo mundo")
            @RequestParam(defaultValue = "false") boolean todasIdades) {
        PlanilhaGerada planilha = relatorioService.necessidadesXlsx(comunidadeId, municipioId, todasIdades);

        ContentDisposition disposicao = ContentDisposition.attachment()
                .filename(planilha.nomeArquivo(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .body(planilha.conteudo());
    }
}
