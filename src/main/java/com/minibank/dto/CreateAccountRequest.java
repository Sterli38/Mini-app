package com.minibank.dto;

public class CreateAccountRequest {
    private Long clientId;
    private String accountNumber;

    public CreateAccountRequest() {
    }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
}
