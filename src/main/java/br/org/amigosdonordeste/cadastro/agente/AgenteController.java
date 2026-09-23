package br.org.amigosdonordeste.cadastro.agente;

import br.org.amigosdonordeste.cadastro.agente.dto.AtivacaoAgenteResposta;
import br.org.amigosdonordeste.cadastro.agente.dto.AtivarAgenteRequisicao;
import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "agentes")
@RestController
@RequestMapping("/api/agentes")
public class AgenteController {

    private final AtivacaoAgenteService ativacao;

    public AgenteController(AtivacaoAgenteService ativacao) {
        this.ativacao = ativacao;
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
