package br.org.amigosdonordeste.cadastro.pessoa;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import br.org.amigosdonordeste.cadastro.pessoa.dto.PessoaFiltroDTO;
import br.org.amigosdonordeste.cadastro.pessoa.request.PessoaRequisicao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Tela de pessoas: listagem com busca e filtros, e o modal de criar/editar.
 * Não existe POST /api/pessoas solto — pessoa não existe fora de uma família,
 * por isso a criação fica em /api/familias/{familiaId}/pessoas.
 */
@Tag(name = "pessoas", description = "Membros das famílias: listagem, ficha, inclusão, edição e remoção")
@RestController
@RequestMapping("/api")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    @Operation(summary = "Lista as pessoas com busca, filtros e paginação",
        description = "Pessoas de famílias inativas não aparecem. A idade é calculada na hora; "
            + "idadeEstimada = true indica que ela veio de estimativa, não de data de nascimento.")
    @Parameters({
        @Parameter(name = "nome", in = ParameterIn.QUERY,
            description = "Trecho do nome; acento e maiúscula não importam (\"jose\" acha \"José\")"),
        @Parameter(name = "comunidadeId", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "uuid"),
            description = "Comunidade da família da pessoa"),
        @Parameter(name = "municipioId", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "uuid"),
            description = "Município da comunidade"),
        @Parameter(name = "familiaId", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "uuid"),
            description = "Só os membros desta família"),
        @Parameter(name = "cadastroIncompleto", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "true: só os incompletos. false: só os completos. Ausente: todos"),
        @Parameter(name = "estuda", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "Quem não tem a informação não entra em nenhum dos dois"),
        @Parameter(name = "faixaEtaria", in = ParameterIn.QUERY,
            schema = @Schema(type = "string", allowableValues = {"ATE_12", "DE_13_A_59", "DE_60_OU_MAIS"}),
            description = "Calculada no banco; quem não tem idade conhecida não entra em faixa nenhuma"),
        @Parameter(name = "pagina", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0"),
            description = "Começa em 0"),
        @Parameter(name = "tamanho", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "20"),
            description = "Padrão 20, teto 100")
    })
    @GetMapping("/pessoas")
    public PaginaResposta<PessoaResumoResponse> listar(@Parameter(hidden = true) PessoaFiltroDTO filtro) {
        return pessoaService.listar(filtro);
    }

    @Operation(summary = "Ficha completa da pessoa, com família, comunidade e município")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pessoa encontrada"),
        @ApiResponse(responseCode = "404", description = "Não existe pessoa com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @GetMapping("/pessoas/{id}")
    public PessoaDetalheResponse buscarPorId(@PathVariable UUID id) {
        return pessoaService.buscarPorId(id);
    }

    @Operation(summary = "Acrescenta uma pessoa a uma família existente",
        description = "Idade: data de nascimento OU idade estimada com idadeEstimadaEm — ou nenhum dos dois. "
            + "Sem nome só com cadastroIncompleto = true.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pessoa criada"),
        @ApiResponse(responseCode = "400", description = "Regra de idade, nome, calçado ou lista fechada quebrada",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "404", description = "Não existe família com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping("/familias/{familiaId}/pessoas")
    @ResponseStatus(HttpStatus.CREATED)
    public PessoaDetalheResponse criar(@PathVariable UUID familiaId, @Valid @RequestBody PessoaRequisicao request) {
        return pessoaService.criar(familiaId, request);
    }

    @Operation(summary = "Edita uma pessoa (mesmas regras da inclusão)",
        description = "Não muda a família: mudar a comunidade de alguém é mudar a família, não a pessoa.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pessoa atualizada"),
        @ApiResponse(responseCode = "400", description = "Regra de idade, nome, calçado ou lista fechada quebrada",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "404", description = "Não existe pessoa com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PutMapping("/pessoas/{id}")
    public PessoaDetalheResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PessoaRequisicao request) {
        return pessoaService.atualizar(id, request);
    }

    @Operation(summary = "Remove a pessoa da família",
        description = "Fonte de renda ligada a ela não é apagada: continua como renda da família, sem pessoa.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pessoa removida"),
        @ApiResponse(responseCode = "404", description = "Não existe pessoa com esse id",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @DeleteMapping("/pessoas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID id) {
        pessoaService.remover(id);
    }
}
