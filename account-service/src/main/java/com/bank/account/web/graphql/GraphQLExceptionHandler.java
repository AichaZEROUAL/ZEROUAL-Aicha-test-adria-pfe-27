package com.bank.account.web.graphql;

import com.bank.account.exception.AccountNotFoundException;
import com.bank.account.exception.InvalidOperationException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GraphQLExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError notFound(AccountNotFoundException exception, DataFetchingEnvironment environment) {
        return GraphQLError.newError()
                .errorType(ErrorType.NOT_FOUND)
                .message(exception.getMessage())
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError invalid(InvalidOperationException exception, DataFetchingEnvironment environment) {
        return GraphQLError.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message(exception.getMessage())
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }
}
