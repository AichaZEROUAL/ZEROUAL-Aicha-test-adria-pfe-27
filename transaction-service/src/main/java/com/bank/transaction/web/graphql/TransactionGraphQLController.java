package com.bank.transaction.web.graphql;

import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.exception.TransactionNotFoundException;
import com.bank.transaction.service.CreateTransactionCommand;
import com.bank.transaction.service.TransactionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class TransactionGraphQLController {

    private final TransactionService transactionService;

    public TransactionGraphQLController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public List<TransactionResponse> transactions() {
        return transactionService.list().stream().map(TransactionResponse::from).toList();
    }

    @QueryMapping
    public TransactionResponse transactionById(@Argument UUID id) {
        try {
            return TransactionResponse.from(transactionService.get(id));
        } catch (TransactionNotFoundException exception) {
            return null;
        }
    }

    @QueryMapping
    public List<TransactionResponse> transactionsByAccount(@Argument UUID accountId) {
        return transactionService.listByAccount(accountId).stream().map(TransactionResponse::from).toList();
    }

    @MutationMapping
    public TransactionResponse createTransaction(@Argument CreateTransactionInput input) {
        return TransactionResponse.from(transactionService.create(new CreateTransactionCommand(
                input.accountId(),
                input.amount(),
                input.currency(),
                input.merchantCategory(),
                input.country())));
    }
}
