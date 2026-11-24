package com.minibank.handler;

import com.minibank.exception.AlreadyExistsException;
import com.minibank.exception.InsufficientFundsException;
import com.minibank.exception.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class BaseHandler implements HttpHandler {
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            process(exchange);
        } catch (JsonProcessingException e) {
            sendError(exchange, 400, "Invalid JSON");
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (NotFoundException e) {
            sendError(exchange, 404, e.getMessage());
        } catch (InsufficientFundsException | AlreadyExistsException e) {
            sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error");
        }
    }

    protected abstract void process(HttpExchange exchange) throws IOException;

    protected <T> T readBody(HttpExchange exchange, Class<T> type) throws IOException {
        try (InputStream body = exchange.getRequestBody()) {
            return objectMapper.readValue(body, type);
        }
    }

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, Map.of("error", message == null ? "Unknown error" : message));
    }
}
