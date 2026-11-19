package com.minibank.handler;

import com.minibank.dto.CreateClientRequest;
import com.minibank.model.Client;
import com.minibank.service.BankService;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class ClientHandler extends BaseHandler {
    private final BankService bankService;

    public ClientHandler(BankService bankService) {
        this.bankService = bankService;
    }

    @Override
    protected void process(HttpExchange exchange) throws IOException {
        CreateClientRequest request = readBody(exchange, CreateClientRequest.class);
        Client client = bankService.registerClient(request.getName(), request.getEmail());

        sendJson(exchange, 201, client);
    }
}
