package br.org.amigosdonordeste.cadastro.familia;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import br.org.amigosdonordeste.cadastro.comum.dto.PaginaResposta;
import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaFiltroDTO;
import br.org.amigosdonordeste.cadastro.familia.dto.OrdenacaoFamilia;
import br.org.amigosdonordeste.cadastro.familia.request.AtualizarFamiliaRequisicao;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "familias")
@RestController
@RequestMapping("/api/familias")
public class FamiliaController {

    private final FamiliaService familiaService;

    public FamiliaController(FamiliaService familiaService) {
        this.familiaService = familiaService;
    }

    @Operation(summary = "Lista as famílias com busca, filtros e paginação; inativas só com incluirInativas=true")
    @Parameters({
        @Parameter(name = "busca", in = ParameterIn.QUERY,
            description = "Trecho do nome da responsável; acento não importa (\"Jose\" acha \"José\")"),
        @Parameter(name = "municipioId", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "uuid")),
        @Parameter(name = "comunidadeId", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "uuid")),
        @Parameter(name = "semBanheiro", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "true: só famílias sem banheiro (ou sem a informação). false ou ausente: sem filtro"),
        @Parameter(name = "incluirInativas", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "true inclui as inativas — é como se acha uma família para reativar"),
        @Parameter(name = "pagina", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0"),
            description = "Começa em 0"),
        @Parameter(name = "porPagina", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "25"),
            description = "Padrão 25, teto 100"),
        @Parameter(name = "estrato", in = ParameterIn.QUERY,
            array = @ArraySchema(schema = @Schema(implementation = EstratoRisco.class)),
            description = "Só famílias destes estratos da avaliação de vulnerabilidade (ADR-0010); pode repetir. "
                + "DADOS_INSUFICIENTES lista quem precisa ter o cadastro completado"),
        @Parameter(name = "ordenacao", in = ParameterIn.QUERY, schema = @Schema(implementation = OrdenacaoFamilia.class),
            description = "NOME (padrão) ou PRIORIDADE: ordem dos estratos e, dentro deles, pontos confirmados. "
                + "É sugestão de leitura, não fila de atendimento")
    })
    @GetMapping
    public PaginaResposta<FamiliaResumoResponse> listar(@Parameter(hidden = true) FamiliaFiltroDTO filtro) {
        return familiaService.listar(filtro);
    }

    @Operation(summary = "Ficha completa da família: comunidade, município, membros, fontes de renda, totais "
        + "e a sugestão de prioridade com a explicação (ADR-0010)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Família encontrada"),
        @ApiResponse(responseCode = "404", description = "Não existe família com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @GetMapping("/{id}")
    public FamiliaDetalheResponse buscarPorId(@PathVariable UUID id) {
        return familiaService.buscarPorId(id);
    }

    @Operation(summary = "Cadastra uma família com membros e fontes de renda numa única chamada")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamiliaResponse criar(@Valid @RequestBody CriarFamiliaRequisicao request) {
        return familiaService.criar(request);
    }

    @Operation(summary = "Atualiza uma família existente, com merge de membros e fontes de renda")
    @PutMapping("/{id}")
    public FamiliaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarFamiliaRequisicao request) {
        return familiaService.atualizar(id, request);
    }

    @Operation(summary = "Inativa a família: some de listagem, contagem, relatório e mapa, mas não é apagada")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Família inativa"),
        @ApiResponse(responseCode = "404", description = "Não existe família com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping("/{id}/inativar")
    public FamiliaResumoResponse inativar(@PathVariable UUID id) {
        return familiaService.inativar(id);
    }

    @Operation(summary = "Reativa uma família inativa: volta a aparecer e a ser contada")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Família ativa"),
        @ApiResponse(responseCode = "404", description = "Não existe família com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping("/{id}/reativar")
    public FamiliaResumoResponse reativar(@PathVariable UUID id) {
        return familiaService.reativar(id);
    }
}