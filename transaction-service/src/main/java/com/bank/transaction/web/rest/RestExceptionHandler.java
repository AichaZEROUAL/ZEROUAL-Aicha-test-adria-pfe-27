package com.bank.transaction.web.rest;

import com.bank.transaction.exception.AccountNotFoundException;
import com.bank.transaction.exception.AccountServiceUnavailableException;
import com.bank.transaction.exception.InvalidTransactionException;
import com.bank.transaction.exception.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = TransactionRestController.class)
public class RestExceptionHandler {

    @ExceptionHandler({TransactionNotFoundException.class, AccountNotFoundException.class})
    public ProblemDetail notFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ProblemDetail unprocessable(InvalidTransactionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ProblemDetail unavailable(AccountServiceUnavailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }
}
