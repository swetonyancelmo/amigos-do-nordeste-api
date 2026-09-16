package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import br.org.amigosdonordeste.cadastro.precadastro.dto.EnviarPreCadastroRequisicao;
import br.org.amigosdonordeste.cadastro.precadastro.dto.EnviarPreCadastroResposta;
import br.org.amigosdonordeste.cadastro.precadastro.dto.PreCadastroResumoResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * O POST e a unica porta do aparelho da agente. SegurancaConfig ja restringe
 * essa rota a ROLE_AGENTE; o @PreAuthorize repete a regra aqui para ela nao
 * depender so da lista de la. O GET e do painel (ADMIN, o padrao de toda rota).
 *
 * O corpo do POST chega como JsonNode, nao como o DTO: e o JSON bruto que fica
 * guardado em pre_cadastro.payload. Se o binding fosse direto no record, um
 * campo que uma versao mais nova do app mandasse e o servidor ainda nao
 * conhecesse sumiria antes de chegar a revisao. A conversao e a validacao do
 * DTO acontecem no service.
 */
@Tag(name = "pre-cadastros")
@RestController
@RequestMapping("/api/pre-cadastros")
public class PreCadastroController {

    private final PreCadastroService service;

    public PreCadastroController(PreCadastroService service) {
        this.service = service;
    }

    @Operation(summary = "Listar a fila de pré-cadastros (tela de Chamados)",
        description = "Cada item traz responsável, comunidade, total de pessoas (calculado), quem enviou, quando "
            + "e possivelDuplicata: família já cadastrada na mesma comunidade com o mesmo telefone ou nome "
            + "parecido (sem acento), ou null. É só aviso — quem decide é a pessoa que revisa.",
        security = @SecurityRequirement(name = "bearer"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista, mais antigo primeiro"),
        @ApiResponse(responseCode = "401", description = "Sem token de administrador",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PreCadastroResumoResposta> listar(
            @io.swagger.v3.oas.annotations.Parameter(description = "Situação da fila; sem ela, lista todas")
            @RequestParam(required = false) SituacaoPreCadastro situacao) {
        return service.listar(situacao);
    }

    @Operation(summary = "Enviar um pré-cadastro do aparelho da agente",
        description = "Idempotente pelo id gerado no aparelho: reenviar o mesmo id responde JA_RECEBIDO "
            + "com 200 e não cria outro registro. O app trata ACEITO e JA_RECEBIDO como sucesso.",
        security = @SecurityRequirement(name = "bearer"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recebido (ACEITO) ou já estava no servidor (JA_RECEBIDO)"),
        @ApiResponse(responseCode = "400", description = "Payload inválido",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "401", description = "Token do aparelho ausente, desconhecido ou de agente desativada",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('AGENTE')")
    public EnviarPreCadastroResposta enviar(@AuthenticationPrincipal String agenteId,
                                            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                content = @Content(schema = @Schema(implementation = EnviarPreCadastroRequisicao.class)))
                                            @RequestBody JsonNode corpo) {
        return service.receber(UUID.fromString(agenteId), corpo);
    }
}
