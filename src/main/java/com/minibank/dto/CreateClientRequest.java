package com.minibank.dto;

public class CreateClientRequest {
    private String name;
    private String email;

    public CreateClientRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
