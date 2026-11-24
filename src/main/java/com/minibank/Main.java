package com.minibank;

import com.minibank.config.JpaConfig;
import com.minibank.exception.NotFoundException;
import com.minibank.dto.AccountResponse;
import com.minibank.dto.ClientResponse;
import com.minibank.service.BankService;

import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        BankService bank = new BankService(JpaConfig.entityManagerFactory());

        long run = System.currentTimeMillis();

        ClientResponse client = bank.registerClient("Ivan Petrov", "ivan" + run + "@example.com");
        AccountResponse account1 = bank.openAccount(client.getId(), "ACC-" + run + "-1");
        AccountResponse account2 = bank.openAccount(client.getId(), "ACC-" + run + "-2");

        bank.deposit(account1.getId(), new BigDecimal("1000.00"));

        System.out.println("=== Обычный перевод ===");
        bank.transfer(account1.getId(), account2.getId(), new BigDecimal("100.00"));
        System.out.println("account1 balance: " + bank.getAccount(account1.getId()).getBalance());
        System.out.println("account2 balance: " + bank.getAccount(account2.getId()).getBalance());

        System.out.println();
        System.out.println("=== Перевод на несуществующий счёт (баг модуля 4–5, теперь исправлен) ===");
        BigDecimal balanceBefore = bank.getAccount(account1.getId()).getBalance();
        try {
            bank.transfer(account1.getId(), 999_999L, new BigDecimal("50.00"));
        } catch (NotFoundException e) {
            System.out.println("Ошибка (как и ожидалось): " + e.getMessage());
        }
        BigDecimal balanceAfter = bank.getAccount(account1.getId()).getBalance();
        System.out.println("Баланс до попытки перевода: " + balanceBefore);
        System.out.println("Баланс после попытки перевода: " + balanceAfter);
        System.out.println("Деньги пропали: " + (balanceAfter.compareTo(balanceBefore) < 0));

        JpaConfig.close();
    }
}
