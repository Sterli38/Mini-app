package com.minibank.handler;

import com.minibank.dto.TransferRequest;
import com.minibank.service.BankService;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class TransferHandler extends BaseHandler {
    private final BankService bankService;

    public TransferHandler(BankService bankService) {
        this.bankService = bankService;
    }

    @Override
    protected void process(HttpExchange exchange) throws IOException {
        TransferRequest request = readBody(exchange, TransferRequest.class);
        bankService.transfer(request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        sendJson(exchange, 200, Map.of("status", "OK"));
    }
}
