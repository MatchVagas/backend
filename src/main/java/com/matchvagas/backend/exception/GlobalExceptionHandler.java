package com.matchvagas.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // CORRIGIDO: adicionado @ExceptionHandler — estava faltando, causava 403 em vez de 400
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        log.warn("Regra de negócio violada: {}", exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erro na regra de negócio",
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception) {
        log.warn("Recurso não encontrado: {}", exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Recurso não encontrado",
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        log.warn("Campo multipart ausente: {}", ex.getRequestPartName());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Campo obrigatório ausente",
                "O campo '" + ex.getRequestPartName() + "' é obrigatório e não foi enviado.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                "Um ou mais campos estão inválidos",
                LocalDateTime.now(),
                errors
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("Acesso negado: {}", exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Acesso negado",
                "Você não tem permissão para acessar este recurso.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(RateLimitException exception) {
        log.warn("Limite de requisições excedido: {}", exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Muitas requisições",
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(LlmIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleLlmIndisponivel(LlmIndisponivelException exception) {
        log.warn("Assistente de IA indisponível: {}", exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Assistente de IA indisponível",
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException exception) {
        log.warn("Falha de autenticação: {}", exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Não autorizado",
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    // Erros de requisição detectados pelo próprio Spring MVC. Sem estes handlers cairiam no
    // fallback abaixo e virariam 500 — escondendo erro do cliente e poluindo o log de erros.
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            MissingServletRequestParameterException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleErroDeRequisicao(Exception exception) {
        HttpStatus status = HttpStatus.valueOf(
                ((org.springframework.web.ErrorResponse) exception).getStatusCode().value());
        String mensagem = switch (exception) {
            case HttpRequestMethodNotSupportedException e -> "Método " + e.getMethod() + " não suportado nesta rota.";
            case HttpMediaTypeNotSupportedException e -> "Tipo de conteúdo não suportado: " + e.getContentType() + ".";
            case MissingServletRequestParameterException e ->
                    "O parâmetro '" + e.getParameterName() + "' é obrigatório e não foi enviado.";
            case NoResourceFoundException e -> "Rota não encontrada.";
            default -> status.getReasonPhrase();
        };
        log.warn("Requisição inválida ({}): {}", status.value(), exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(status.value(), status.getReasonPhrase(), mensagem, LocalDateTime.now()), status);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCorpoIlegivel(HttpMessageNotReadableException exception) {
        log.warn("Corpo da requisição ilegível: {}", exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Corpo da requisição inválido",
                "O corpo da requisição está ausente ou não é um JSON válido.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException exception) {
        log.warn("Parâmetro com tipo inválido: {}", exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Parâmetro inválido",
                "Valor inválido para o parâmetro '" + exception.getName() + "'.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Campo obrigatório ausente ou valor duplicado que só o banco detectou. A mensagem do
    // driver (com SQL e nomes de colunas) fica só no log.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleViolacaoDeIntegridade(DataIntegrityViolationException exception) {
        log.warn("Violação de integridade dos dados: {}", exception.getMostSpecificCause().getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Dados inválidos ou conflitantes",
                "A operação viola uma restrição dos dados: há campo obrigatório ausente ou valor já cadastrado.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // Fallback para exceções inesperadas
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        if (isClientAbort(exception)) {
            log.debug("Cliente encerrou a conexão antes da resposta ser enviada: {}", exception.getMessage());
            return null;
        }
        // Identificador de correlação: o mesmo id vai para o log e para a resposta,
        // permitindo rastrear um erro reportado pelo usuário até o stack trace.
        // É também o ponto de integração para um error tracking (ex.: Sentry).
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Erro inesperado [errorId={}]: {}", errorId, exception.getMessage(), exception);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno do servidor",
                "Ocorreu um erro inesperado. Tente novamente mais tarde. Referência: " + errorId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean isClientAbort(Throwable ex) {
        if (ex == null) return false;
        String className = ex.getClass().getName();
        if (className.contains("ClientAbortException")) return true;
        String message = ex.getMessage();
        if (ex instanceof IOException && message != null && message.contains("Broken pipe")) return true;
        return isClientAbort(ex.getCause());
    }
}
