package com.bank.transaction.web.graphql;

import com.bank.transaction.exception.InvalidTransactionException;
import com.bank.transaction.exception.TransactionNotFoundException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GraphQLExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError notFound(TransactionNotFoundException exception, DataFetchingEnvironment environment) {
        return GraphQLError.newError()
                .errorType(ErrorType.NOT_FOUND)
                .message(exception.getMessage())
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError invalid(InvalidTransactionException exception, DataFetchingEnvironment environment) {
        return GraphQLError.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message(exception.getMessage())
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }
}
