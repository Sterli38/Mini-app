package com.minibank.handler;

import com.minibank.dto.CreateAccountRequest;
import com.minibank.model.Account;
import com.minibank.service.BankService;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class AccountHandler extends BaseHandler {
    private final BankService bankService;

    public AccountHandler(BankService bankService) {
        this.bankService = bankService;
    }

    @Override
    protected void process(HttpExchange exchange) throws IOException {
        CreateAccountRequest request = readBody(exchange, CreateAccountRequest.class);
        Account account = bankService.openAccount(request.getClientId(), request.getAccountNumber());

        sendJson(exchange, 201, account);
    }
}
