package com.bank.transaction.web.graphql;

import com.bank.transaction.exception.AccountNotFoundException;
import com.bank.transaction.exception.AccountServiceUnavailableException;
import com.bank.transaction.exception.InvalidTransactionException;
import com.bank.transaction.exception.TransactionNotFoundException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GraphQLExceptionHandler {

    @GraphQlExceptionHandler({TransactionNotFoundException.class, AccountNotFoundException.class})
    public GraphQLError notFound(RuntimeException exception, DataFetchingEnvironment environment) {
        return error(ErrorType.NOT_FOUND, exception, environment);
    }

    @GraphQlExceptionHandler
    public GraphQLError invalid(InvalidTransactionException exception, DataFetchingEnvironment environment) {
        return error(ErrorType.BAD_REQUEST, exception, environment);
    }

    @GraphQlExceptionHandler
    public GraphQLError unavailable(AccountServiceUnavailableException exception, DataFetchingEnvironment environment) {
        return error(ErrorType.INTERNAL_ERROR, exception, environment);
    }

    private GraphQLError error(ErrorType type, RuntimeException exception, DataFetchingEnvironment environment) {
        return GraphQLError.newError()
                .errorType(type)
                .message(exception.getMessage())
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }
}
