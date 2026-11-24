package com.minibank.service;

import com.minibank.dto.AccountResponse;
import com.minibank.dto.ClientResponse;
import com.minibank.dto.ClientSummary;
import com.minibank.exception.AlreadyExistsException;
import com.minibank.exception.InsufficientFundsException;
import com.minibank.exception.NotFoundException;
import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.model.Transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public class BankService {
    private static final String UNIQUE_VIOLATION = "23505";
    private static final int ACCOUNT_NUMBER_MAX_LENGTH = 34;

    private final EntityManagerFactory emf;

    public BankService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public ClientResponse registerClient(String name, String email) {
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("name and email are required");
        }

        try {
            return inTransaction(em -> {
                Client client = new Client(name, email);
                em.persist(client);
                em.flush();
                return ClientResponse.from(client);
            });
        } catch (RuntimeException e) {
            if (isUniqueViolation(e)) {
                throw new AlreadyExistsException("Client with email already exists: " + email);
            }
            throw e;
        }
    }

    public AccountResponse openAccount(Long clientId, String accountNumber) {
        if (clientId == null || accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("clientId and accountNumber are required");
        }
        if (accountNumber.length() > ACCOUNT_NUMBER_MAX_LENGTH) {
            throw new IllegalArgumentException("accountNumber must be at most " + ACCOUNT_NUMBER_MAX_LENGTH + " characters");
        }

        try {
            return inTransaction(em -> {
                Client client = findClient(em, clientId);
                Account account = new Account(client, accountNumber);
                em.persist(account);
                em.flush();
                return AccountResponse.from(account);
            });
        } catch (RuntimeException e) {
            if (isUniqueViolation(e)) {
                throw new AlreadyExistsException("Account number already exists: " + accountNumber);
            }
            throw e;
        }
    }

    public AccountResponse getAccount(Long accountId) {
        return inTransaction(em -> AccountResponse.from(findAccount(em, accountId)));
    }

    public ClientResponse getClient(Long clientId) {
        return inTransaction(em -> ClientResponse.from(findClient(em, clientId)));
    }

    public List<ClientSummary> getClientSummariesWithNPlusOne() {
        return inTransaction(em -> em.createQuery("select c from Client c", Client.class)
                .getResultList()
                .stream()
                .map(c -> new ClientSummary(c.getId(), c.getName(), c.getAccounts().size()))
                .toList());
    }

    public List<ClientSummary> getClientSummaries() {
        return inTransaction(em -> em.createQuery("select c from Client c left join fetch c.accounts", Client.class)
                .getResultList()
                .stream()
                .map(c -> new ClientSummary(c.getId(), c.getName(), c.getAccounts().size()))
                .toList());
    }

    public void deposit(Long accountId, BigDecimal amount) {
        inTransaction(em -> {
            Account account = findAccount(em, accountId);
            account.setBalance(account.getBalance().add(amount));
            em.persist(new Transaction(account, amount, "DEPOSIT"));
            return null;
        });
    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId == null || toAccountId == null || amount == null) {
            throw new IllegalArgumentException("fromAccountId, toAccountId and amount are required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        inTransaction(em -> {
            Account from = findAccount(em, fromAccountId);
            Account to = findAccount(em, toAccountId);

            if (from.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds on account " + fromAccountId);
            }

            from.setBalance(from.getBalance().subtract(amount));
            to.setBalance(to.getBalance().add(amount));
            em.persist(new Transaction(from, amount.negate(), "TRANSFER_OUT"));
            em.persist(new Transaction(to, amount, "TRANSFER_IN"));
            return null;
        });
    }

    private Account findAccount(EntityManager em, Long accountId) {
        Account account = em.find(Account.class, accountId);
        if (account == null) {
            throw new NotFoundException("Account not found: " + accountId);
        }
        return account;
    }

    private Client findClient(EntityManager em, Long clientId) {
        Client client = em.find(Client.class, clientId);
        if (client == null) {
            throw new NotFoundException("Client not found: " + clientId);
        }
        return client;
    }

    private <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    private boolean isUniqueViolation(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve && UNIQUE_VIOLATION.equals(cve.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
