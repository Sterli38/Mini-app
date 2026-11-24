package com.minibank.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class JpaConfig {
    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("minibank");

    private JpaConfig() {
    }

    public static EntityManagerFactory entityManagerFactory() {
        return EMF;
    }

    public static void close() {
        if (EMF.isOpen()) {
            EMF.close();
        }
    }
}
