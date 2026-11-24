# Модуль 6 — N+1 до и после

Запуск: `com.minibank.demo.NPlusOneDemo` (1004 клиента, `hibernate.generate_statistics=true`).

| Вариант | Метод | SQL-запросов | Время выполнения запросов |
|---|---|---|---|
| ДО (N+1) | `BankService.getClientSummariesWithNPlusOne()` | 1005 | ~758 мс |
| ПОСЛЕ (JOIN FETCH) | `BankService.getClientSummaries()` | 1 | ~3,5 мс |

## ДО
JPQL: `select c from Client c`, связь `@OneToMany(mappedBy = "client")` — LAZY.

```sql
select c1_0.id,c1_0.email,c1_0.name from clients c1_0
-- и 1004 раза (на каждый c.getAccounts().size()):
select a1_0.client_id,a1_0.id,a1_0.account_number,a1_0.balance from accounts a1_0 where a1_0.client_id=?
```

## ПОСЛЕ
JPQL: `select c from Client c left join fetch c.accounts`

```sql
select c1_0.id,a1_0.client_id,a1_0.id,a1_0.account_number,a1_0.balance,c1_0.email,c1_0.name
from clients c1_0 left join accounts a1_0 on c1_0.id=a1_0.client_id
```
