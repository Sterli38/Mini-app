package com.minibank;

import com.minibank.config.ConnectionFactory;
import com.minibank.handler.AccountHandler;
import com.minibank.handler.ClientHandler;
import com.minibank.handler.TransferHandler;
import com.minibank.service.BankService;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerApp {
    public static void main(String[] args) throws IOException {
        BankService bankService = new BankService(new ConnectionFactory());

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/transfer", new TransferHandler(bankService));
        server.createContext("/clients", new ClientHandler(bankService));
        server.createContext("/accounts", new AccountHandler(bankService));
        server.start();

        System.out.println("Сервер запущен: http://localhost:8080");
    }
}
