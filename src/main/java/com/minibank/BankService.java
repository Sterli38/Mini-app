package com.minibank;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MiniBank — "MVP", который наспех собрал предыдущий разработчик:
 * вся логика в одном классе, все данные — в HashMap, персистентности
 * нет вообще (перезапуск процесса — и всё пропало).
 *
 * ПРОБЛЕМА (найдёшь и исправишь, когда дойдёт очередь — модуль 7.1,
 * когда появится @Transactional): в transfer() проверка существования
 * счёта-получателя происходит ПОСЛЕ списания денег со счёта-
 * отправителя. Если счёт-получатель не существует, деньги уже спишутся
 * с первого счёта и "исчезнут" — их некуда вернуть, потому что метод
 * прерывается исключением до зачисления.
 *
 * Эта проблема не решается на HashMap "в лоб" без искусственных
 * костылей (можно вручную откатывать баланс в catch, но это не
 * настоящее решение) — она по-настоящему решается только когда
 * появится транзакционность (сначала в БД — ACID, модуль 4, а затем
 * в коде — @Transactional, модуль 7.1). До этого момента баг просто
 * существует и это нормально: то, что появится в модуле 7.1, — как
 * раз тот инструмент, который проблему полностью закрывает.
 */
public class BankService {
    private final ConnectionFactory connectionFactory;

    public BankService(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Client registerClient(String name, String email) {
        String sql = "INSERT INTO clients (name, email) VALUES (?, ?) RETURNING id";
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Client(rs.getLong("id"), name, email);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register client", e);
        }
    }

    public Account openAccount(Long clientId, String accountNumber) {
        getClient(clientId);

        String sql = "INSERT INTO accounts (client_id, account_number, balance) VALUES (?, ?, 0) RETURNING id";
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, clientId);
            ps.setString(2, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Account(rs.getLong("id"), clientId, accountNumber, BigDecimal.ZERO);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open account", e);
        }
    }

    public Account getAccount(Long accountId) {
        String sql = "SELECT id, client_id, account_number, balance FROM accounts WHERE id = ?";

        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Account not found: " + accountId);
                }
                return new Account(
                        rs.getLong("id"),
                        rs.getLong("client_id"),
                        rs.getString("account_number"),
                        rs.getBigDecimal("balance"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load account " + accountId, e);
        }
    }

    public Client getClient(Long clientId) {
        String sql = "SELECT id, name, email FROM clients WHERE id = ?";

        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Client not found: " + clientId);
                }
                return new Client(rs.getLong("id"), rs.getString("name"), rs.getString("email"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load client " + clientId, e);
        }
    }

    public void deposit(Long accountId, BigDecimal amount) {
        Account account = getAccount(accountId);
        updateBalance(accountId, account.getBalance().add(amount));
        recordTransaction(accountId, amount, "DEPOSIT");
    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account from = getAccount(fromAccountId);

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds on account " + fromAccountId);
        }

        updateBalance(fromAccountId, from.getBalance().subtract(amount));
        recordTransaction(fromAccountId, amount.negate(), "TRANSFER_OUT");

        Account to = getAccount(toAccountId); // если бросит исключение — деньги уже пропали

        updateBalance(toAccountId, to.getBalance().add(amount));
        recordTransaction(toAccountId, amount, "TRANSFER_IN");
    }

    private void updateBalance(Long accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";

        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update balance of account " + accountId, e);
        }
    }

    private void recordTransaction(Long accountId, BigDecimal amount, String type) {
        String sql = "INSERT INTO transactions (account_id, amount, type) VALUES (?, ?, ?)";
        
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, type);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record transaction", e);
        }
    }

    private Connection openConnection() throws SQLException {
        return connectionFactory.getConnection();
    }
}
