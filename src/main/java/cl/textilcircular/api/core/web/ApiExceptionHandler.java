package cl.textilcircular.api.core.web;

import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.DomainException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.model.account.AccountNotActiveException;
import cl.textilcircular.api.core.model.account.InvalidCredentialsException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps every failure to the contract's problem body. Domain messages pass through unchanged. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    ProblemDetail validation(ValidationException e) {
        return Problems.validation(e.getField(), e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException e) {
        return Problems.of(HttpStatus.CONFLICT, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return Problems.of(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage());
    }

    @ExceptionHandler(AccountNotActiveException.class)
    ProblemDetail accountNotActive(AccountNotActiveException e) {
        ProblemDetail problem = Problems.of(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", e.getMessage());
        problem.setProperty("accountStatus", e.getStatus());
        return problem;
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail forbidden(ForbiddenException e) {
        return Problems.of(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail otherDomain(DomainException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    /** Unique keys hit by a concurrent request after the service's own check passed. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataIntegrity(DataIntegrityViolationException e) {
        log.info("Rejected by a database constraint: {}", e.getMostSpecificCause().getMessage());
        return Problems.of(HttpStatus.CONFLICT, "CONFLICT", "Los datos cambiaron mientras guardabas. Inténtalo de nuevo");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail optimisticLock(OptimisticLockingFailureException e) {
        return Problems.of(HttpStatus.CONFLICT, "CONFLICT", "Los datos cambiaron mientras guardabas. Inténtalo de nuevo");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatch(MethodArgumentTypeMismatchException e) {
        return Problems.validation(e.getName(), "El valor no es válido");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("Unexpected error", e);
        return Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Algo salió mal. Inténtalo de nuevo");
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = Problems.of(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", Problems.VALIDATION_DETAIL);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Spring MVC's own errors (404, 405, 415...) get the {@code code} extension too. */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem && (problem.getProperties() == null
                || !problem.getProperties().containsKey("code"))) {
            problem.setProperty("code", Problems.codeFor(statusCode));
            if (statusCode.value() == 404) {
                problem.setDetail(new NotFoundException().getMessage());
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
