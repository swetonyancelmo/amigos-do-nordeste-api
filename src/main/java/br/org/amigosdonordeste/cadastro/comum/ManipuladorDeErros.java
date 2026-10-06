package br.org.amigosdonordeste.cadastro.comum;

import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.JsonMappingException;

import br.org.amigosdonordeste.cadastro.agente.AgenteNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.agente.CodigoConviteInvalidoException;
import br.org.amigosdonordeste.cadastro.auth.CredenciaisInvalidasException;
import br.org.amigosdonordeste.cadastro.auth.SenhaAtualIncorretaException;
import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.exception.CpfInvalidoException;
import br.org.amigosdonordeste.cadastro.familia.exception.CpfJaCadastradoException;
import br.org.amigosdonordeste.cadastro.familia.exception.FamiliaNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdDuplicadoNoPayloadException;
import br.org.amigosdonordeste.cadastro.familia.exception.IdadeEstimadaInvalidaException;
import br.org.amigosdonordeste.cadastro.familia.exception.NumeroCalcadoInvalidoException;
import br.org.amigosdonordeste.cadastro.familia.exception.PessoaReferenciadaInvalidaException;
import br.org.amigosdonordeste.cadastro.municipio.CodigoIbgeJaCadastradoException;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.pessoa.exception.PessoaInvalidaException;
import br.org.amigosdonordeste.cadastro.pessoa.exception.PessoaNaoEncontradaException;
import br.org.amigosdonordeste.cadastro.precadastro.MuitosIdsException;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroInvalidoException;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroJaAvaliadoException;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.usuario.EmailJaCadastradoException;

/**
 * Toda resposta de erro sai no mesmo formato, com a chave `message`, porque e o
 * que o cliente do frontend le. Mensagem em portugues e util: quem vai ler e a
 * usuaria, nao o desenvolvedor.
 */
@RestControllerAdvice
public class ManipuladorDeErros {

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResposta> credenciais(CredenciaisInvalidasException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(CodigoConviteInvalidoException.class)
    public ResponseEntity<ErroResposta> codigoConvite(CodigoConviteInvalidoException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AgenteNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> agenteNaoEncontrado(AgenteNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ErroResposta> muitasTentativas(MuitasTentativasException e) {
        return resposta(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(SenhaAtualIncorretaException.class)
    public ResponseEntity<ErroResposta> senha(SenhaAtualIncorretaException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResposta> emailDuplicado(EmailJaCadastradoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ComunidadeNaoEncontradaException.class)
    public ResponseEntity<ErroResposta> comunidadeNaoEncontrada(ComunidadeNaoEncontradaException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MunicipioNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> municipioNaoEncontrado(MunicipioNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CodigoIbgeJaCadastradoException.class)
    public ResponseEntity<ErroResposta> codigoIbgeDuplicado(CodigoIbgeJaCadastradoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(FamiliaNaoEncontradaException.class)
    public ResponseEntity<ErroResposta> familiaNaoEncontrada(FamiliaNaoEncontradaException e) {
    return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PessoaNaoEncontradaException.class)
    public ResponseEntity<ErroResposta> pessoaNaoEncontrada(PessoaNaoEncontradaException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PessoaInvalidaException.class)
    public ResponseEntity<ErroResposta> pessoaInvalida(PessoaInvalidaException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PessoaReferenciadaInvalidaException.class)
    public ResponseEntity<ErroResposta> pessoaReferenciadaInvalida(PessoaReferenciadaInvalidaException e) {
    return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NumeroCalcadoInvalidoException.class)
    public ResponseEntity<ErroResposta> numeroCalcadoInvalido(NumeroCalcadoInvalidoException e) {
    return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IdadeEstimadaInvalidaException.class)
    public ResponseEntity<ErroResposta> idadeEstimadaInvalida(IdadeEstimadaInvalidaException e) {
    return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IdDuplicadoNoPayloadException.class)
    public ResponseEntity<ErroResposta> idDuplicado(IdDuplicadoNoPayloadException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CpfInvalidoException.class)
    public ResponseEntity<ErroResposta> cpfInvalido(CpfInvalidoException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CpfJaCadastradoException.class)
    public ResponseEntity<ErroResposta> cpfJaCadastrado(CpfJaCadastradoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(PreCadastroInvalidoException.class)
    public ResponseEntity<ErroResposta> preCadastroInvalido(PreCadastroInvalidoException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MuitosIdsException.class)
    public ResponseEntity<ErroResposta> muitosIds(MuitosIdsException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PreCadastroNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> preCadastroNaoEncontrado(PreCadastroNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PreCadastroJaAvaliadoException.class)
    public ResponseEntity<ErroResposta> preCadastroJaAvaliado(PreCadastroJaAvaliadoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Rede de segurança: constraint do banco (tamanho, check, unique) que
     * escapou do Bean Validation. Sem isso viraria 500 com stack trace.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos: violam uma restrição do cadastro.");
    }

    /**
     * JSON que não converte — em geral texto livre num campo de lista fechada
     * (sexo, serie, tamanhoRoupa...). Tratado aqui para sair no formato de
     * sempre, e sem o valor digitado: nem na resposta, nem no log de WARN que
     * o Spring escreveria por padrão.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException e) {
        String campo = null;
        if (e.getCause() instanceof JsonMappingException mapeamento && !mapeamento.getPath().isEmpty()) {
            campo = mapeamento.getPath().get(mapeamento.getPath().size() - 1).getFieldName();
        }
        String mensagem = campo != null
            ? campo + ": valor não aceito. As opções estão em /api/metadados."
            : "Corpo da requisição inválido.";
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    /** Parâmetro de rota/consulta que não converte (ex.: id que não é UUID). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getName() + ": valor inválido.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
            // falha de conversão (ex.: faixaEtaria=QUALQUER na listagem) traz
            // mensagem técnica em inglês — troca por uma que a usuária entenda
            .map(erro -> erro.getField() + ": "
                + (erro.isBindingFailure() ? "valor inválido." : erro.getDefaultMessage()))
            .findFirst()
            .orElse("Dados inválidos.");
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem) {
        ErroResposta corpo = new ErroResposta(OffsetDateTime.now().toString(), status.value(), mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
