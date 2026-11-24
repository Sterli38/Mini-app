package com.minibank.demo;

import com.minibank.config.JpaConfig;
import com.minibank.dto.ClientSummary;
import com.minibank.service.BankService;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.List;

/** Показывает проблему N+1 и её исправление через JOIN FETCH. */
public class NPlusOneDemo {
    public static void main(String[] args) {
        EntityManagerFactory emf = JpaConfig.entityManagerFactory();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        BankService bankService = new BankService(emf);

        stats.clear();
        List<ClientSummary> before = bankService.getClientSummariesWithNPlusOne();
        long queriesBefore = stats.getPrepareStatementCount();

        stats.clear();
        List<ClientSummary> after = bankService.getClientSummaries();
        long queriesAfter = stats.getPrepareStatementCount();

        System.out.println("ДО   (N+1):        клиентов " + before.size() + ", SQL-запросов " + queriesBefore);
        System.out.println("ПОСЛЕ (JOIN FETCH): клиентов " + after.size() + ", SQL-запросов " + queriesAfter);
        JpaConfig.close();
    }
}
