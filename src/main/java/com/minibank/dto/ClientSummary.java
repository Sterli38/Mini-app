package com.minibank.dto;

import java.util.Objects;

public class ClientSummary {
    private final Long id;
    private final String name;
    private final int accountsCount;

    public ClientSummary(Long id, String name, int accountsCount) {
        this.id = id;
        this.name = name;
        this.accountsCount = accountsCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getAccountsCount() { return accountsCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientSummary that)) return false;
        return accountsCount == that.accountsCount
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, accountsCount);
    }

    @Override
    public String toString() {
        return "ClientSummary{id=" + id + ", name='" + name + "', accountsCount=" + accountsCount + "}";
    }
}
