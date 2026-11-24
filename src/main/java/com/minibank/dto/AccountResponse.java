package com.minibank.dto;

import com.minibank.model.Account;

import java.math.BigDecimal;
import java.util.Objects;

public class AccountResponse {
    private final Long id;
    private final Long clientId;
    private final String accountNumber;
    private final BigDecimal balance;

    public AccountResponse(Long id, Long clientId, String accountNumber, BigDecimal balance) {
        this.id = id;
        this.clientId = clientId;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getClient().getId(),
                account.getAccountNumber(),
                account.getBalance());
    }

    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountResponse that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(clientId, that.clientId)
                && Objects.equals(accountNumber, that.accountNumber)
                && Objects.equals(balance, that.balance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clientId, accountNumber, balance);
    }

    @Override
    public String toString() {
        return "AccountResponse{id=" + id + ", clientId=" + clientId
                + ", accountNumber='" + accountNumber + "', balance=" + balance + "}";
    }
}
