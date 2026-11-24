# EXPLAIN ANALYZE: до и после индекса

## Данные
- `clients` — 1 000, `accounts` — 1 000, `transactions` — 100 000 (случайный счёт, сумма и дата за последний год).
- Генерация: Liquibase changeSet `leonid:4` (`changes/test-data.sql`), `generate_series` + `random()`, затем `ANALYZE transactions`.

## Индекс
```sql
CREATE INDEX idx_transactions_account_id_transaction_date
    ON transactions (account_id, transaction_date);
```
Составной: сначала колонка с равенством (`account_id = ?`), потом колонка с диапазоном (`transaction_date >= ?`).
Внутри одного счёта записи индекса уже отсортированы по дате, поэтому период берётся одним непрерывным куском.

## Запрос «все транзакции счёта за период»
```sql
EXPLAIN ANALYZE
SELECT * FROM transactions
WHERE account_id = 42
  AND transaction_date >= now() - interval '3 days'
ORDER BY transaction_date;
```

### ДО индекса — Seq Scan
(индекс удалён внутри транзакции `BEGIN; DROP INDEX ...; EXPLAIN ANALYZE ...; ROLLBACK;`)
```
Sort  (cost=2834.01..2834.02 rows=1 width=38) (actual time=11.717..11.719 rows=1 loops=1)
  Sort Key: transaction_date
  Sort Method: quicksort  Memory: 25kB
  ->  Seq Scan on transactions  (cost=0.00..2834.00 rows=1 width=38) (actual time=0.769..11.706 rows=1 loops=1)
        Filter: ((account_id = 42) AND (transaction_date >= (now() - '3 days'::interval)))
        Rows Removed by Filter: 99999
Planning Time: 0.154 ms
Execution Time: 11.744 ms
```

### ПОСЛЕ индекса — Index Scan
```
Index Scan using idx_transactions_account_id_transaction_date on transactions  (cost=0.42..8.44 rows=1 width=38) (actual time=0.038..0.041 rows=1 loops=1)
  Index Cond: ((account_id = 42) AND (transaction_date >= (now() - '3 days'::interval)))
Planning Time: 0.136 ms
Execution Time: 0.059 ms
```

### Итог
| | План | Прочитано зря | Время |
|---|---|---|---|
| До | Seq Scan + Sort | 99 999 строк | 11.7 ms |
| После | Index Scan (сортировка не нужна — индекс уже упорядочен) | 0 | 0.06 ms |

Ускорение примерно в 200 раз.

## Дополнительно: период 30 дней
```sql
EXPLAIN ANALYZE
SELECT * FROM transactions
WHERE account_id = 42
  AND transaction_date >= now() - interval '30 days';
```
До индекса:
```
Seq Scan on transactions  (cost=0.00..2834.00 rows=8 width=38) (actual time=0.196..25.795 rows=6 loops=1)
  Filter: ((account_id = 42) AND (transaction_date >= (now() - '30 days'::interval)))
  Rows Removed by Filter: 99994
Planning Time: 1.083 ms
Execution Time: 25.918 ms
```
После индекса:
```
Bitmap Heap Scan on transactions  (cost=4.50..34.31 rows=8 width=38) (actual time=0.215..0.248 rows=6 loops=1)
  Recheck Cond: ((account_id = 42) AND (transaction_date >= (now() - '30 days'::interval)))
  Heap Blocks: exact=6
  ->  Bitmap Index Scan on idx_transactions_account_id_transaction_date  (cost=0.00..4.50 rows=8 width=0) (actual time=0.198..0.199 rows=6 loops=1)
        Index Cond: ((account_id = 42) AND (transaction_date >= (now() - '30 days'::interval)))
Planning Time: 1.015 ms
Execution Time: 0.359 ms
```
Здесь планировщик выбрал **Bitmap Index Scan** — это тоже использование индекса. 6 найденных строк лежат на 6 разных страницах,
поэтому Postgres сначала собирает по индексу карту страниц, а потом читает их по порядку — так дешевле, чем 6 случайных переходов.
Обычный Index Scan выбирается, когда строк совсем мало (как в запросе на 3 дня) или нужна сортировка по индексу.
