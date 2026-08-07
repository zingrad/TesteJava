package br.com.srm.creditengine.api.error;

import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import br.com.srm.creditengine.domain.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiErrorHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    private static final Comparator<FieldViolation> BY_FIELD =
            Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail onResourceNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), ex.code());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail onBusinessRule(BusinessRuleException ex) {
        log.info("Business rule rejected the request [code={}]: {}", ex.code(), ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio violada", ex.getMessage(), ex.code());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail onOptimisticLocking(OptimisticLockingFailureException ex) {
        log.warn("Concurrent modification detected", ex);
        return problem(HttpStatus.CONFLICT, "Conflito de concorrencia",
                "O registro foi alterado por outra operacao. Recarregue os dados e tente novamente.",
                "CONCURRENT_MODIFICATION");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail onDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Database rejected the write", ex);
        return problem(HttpStatus.CONFLICT, "Conflito de dados",
                "A operacao viola uma restricao de integridade dos dados.",
                "DATA_INTEGRITY_VIOLATION");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail onConstraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(lastNode(violation), violation.getMessage()))
                .sorted(BY_FIELD)
                .toList();
        return validationProblem(violations);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception ex) {
        String incident = UUID.randomUUID().toString();
        log.error("Unhandled failure [incident={}]", incident, ex);

        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Falha inesperada ao processar a requisicao. Informe o codigo do incidente ao suporte.",
                "INTERNAL_ERROR");
        problem.setProperty("incident", incident);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), messageOf(error)))
                .sorted(BY_FIELD)
                .toList();
        return handleExceptionInternal(ex, validationProblem(violations), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldViolation> violations = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .sorted(BY_FIELD)
                .toList();
        return handleExceptionInternal(ex, validationProblem(violations), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "Endpoint nao encontrado",
                "Nenhum endpoint atende %s %s.".formatted(ex.getHttpMethod(), absolute(ex.getResourcePath())),
                "ENDPOINT_NOT_FOUND");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.NOT_FOUND, request);
    }

    /**
     * As excecoes que o proprio Spring resolve trazem o {@link ProblemDetail} pronto, sem passar por
     * {@code createProblemDetail}. Enriquecer aqui garante que toda resposta de erro tenha o mesmo contrato.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        if (body instanceof ProblemDetail problem) {
            if (!hasProperty(problem, "code")) {
                problem.setProperty("code", codeFor(status));
            }
            if (!hasProperty(problem, "timestamp")) {
                problem.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
            }
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private ProblemDetail validationProblem(List<FieldViolation> violations) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Requisicao invalida",
                "Um ou mais campos falharam na validacao.", "VALIDATION_FAILED");
        problem.setProperty("violations", violations);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
        return problem;
    }

    private static String absolute(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static boolean hasProperty(ProblemDetail problem, String name) {
        return problem.getProperties() != null && problem.getProperties().containsKey(name);
    }

    private static String codeFor(HttpStatusCode status) {
        return status.is5xxServerError() ? "INTERNAL_ERROR" : HttpStatus.valueOf(status.value()).name();
    }

    private static String messageOf(FieldError error) {
        return error.getDefaultMessage() == null ? "valor invalido" : error.getDefaultMessage();
    }

    private static String lastNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        return separator < 0 ? path : path.substring(separator + 1);
    }
}
