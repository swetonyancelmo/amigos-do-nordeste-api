package br.org.amigosdonordeste.cadastro.agente;

import br.org.amigosdonordeste.cadastro.agente.dto.AgenteResposta;
import br.org.amigosdonordeste.cadastro.agente.dto.AtivacaoAgenteResposta;
import br.org.amigosdonordeste.cadastro.agente.dto.AtivarAgenteRequisicao;
import br.org.amigosdonordeste.cadastro.agente.dto.CriarAgenteRequisicao;
import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * /ativar e do aparelho (publica, com limite por IP — ver SegurancaConfig).
 * O resto e do painel e fica no padrao ADMIN de toda rota.
 */
@Tag(name = "agentes")
@RestController
@RequestMapping("/api/agentes")
public class AgenteController {

    private final AtivacaoAgenteService ativacao;
    private final ConviteAgenteService convites;

    public AgenteController(AtivacaoAgenteService ativacao, ConviteAgenteService convites) {
        this.ativacao = ativacao;
        this.convites = convites;
    }

    @Operation(summary = "Cadastrar uma agente e gerar o código de convite",
        description = "Devolve o código de seis dígitos que a agente digita no app. Ele vale uma vez.",
        security = @SecurityRequirement(name = "bearer"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agente cadastrada, com o código de convite"),
        @ApiResponse(responseCode = "400", description = "Nome ausente ou longo demais",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgenteResposta criar(@Valid @RequestBody CriarAgenteRequisicao requisicao) {
        return convites.criar(requisicao.nome());
    }

    @Operation(summary = "Listar as agentes, com o convite ainda pendente de cada uma",
        security = @SecurityRequirement(name = "bearer"))
    @GetMapping
    public List<AgenteResposta> listar() {
        return convites.listar();
    }

    @Operation(summary = "Gerar um código de convite novo para a mesma agente",
        description = "Para celular perdido, trocado ou com o app reinstalado. O aparelho antigo perde o acesso "
            + "na hora; os pré-cadastros já enviados continuam ligados à agente.",
        security = @SecurityRequirement(name = "bearer"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Código novo gerado"),
        @ApiResponse(responseCode = "404", description = "Agente não encontrada",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping("/{id}/novo-convite")
    public AgenteResposta novoConvite(@PathVariable UUID id) {
        return convites.novoConvite(id);
    }

    @Operation(summary = "Ativar o aparelho da agente",
        description = "Troca o código de convite de seis dígitos pelo token do aparelho. "
            + "O código é de uso único e o token é entregue uma única vez — guarde-o no aparelho.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Aparelho ativado"),
        @ApiResponse(responseCode = "400", description = "Código fora do formato",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "401", description = "Código inválido (inexistente ou já usado)",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "429", description = "Muitas tentativas deste IP",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping("/ativar")
    public AtivacaoAgenteResposta ativar(@Valid @RequestBody AtivarAgenteRequisicao requisicao,
                                         HttpServletRequest http) {
        return ativacao.ativar(requisicao.codigo(), http.getRemoteAddr());
    }
}
