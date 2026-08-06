# Collections Architecture

This document explains the data structure choices behind the Collections Framework migration
in Phase Three, the reasoning for each one, and how they interact with the functional,
persistence, and concurrency work built on top of them.

## What arrays could not do

Both `AccountManager` and `TransactionManager` originally stored their data in fixed-size
arrays (`Account[50]`, `Transaction[200]`), with a separate counter field tracking how many
slots were actually in use. This had two concrete problems this migration removes:

- **A hard capacity ceiling.** A 51st account, or a 201st transaction, simply could not be
  added; `addAccount`/`addTransaction` had to check the array's length before every insertion
  and silently refuse once full.
- **Linear search cost.** `findAccount` had to walk the array from index 0 until it found a
  matching account number, an O(n) operation whose cost grows with every account ever created.

## `AccountManager`: `HashMap<String, Account>`

```java
private Map<String, Account> accountsMap = new HashMap<>();
```

Accounts have a natural, unique identifier, the account number, and the dominant operation on
this collection is "retrieve the one account with this number" (`findAccount`, called from
every console flow that touches an existing account). A `HashMap` computes the bucket for a
key directly from its `hashCode()`, giving average-case O(1) lookup regardless of how many
accounts exist, replacing the array's O(n) scan entirely. `String`'s `hashCode()` and
`equals()` compare by character content, not object identity, so two separately-constructed
`"ACC003"` strings are correctly treated as the same key.

`addAccount` relies on `Map.put`'s return value, the previous value for that key, or `null` if
the key was new, to detect a duplicate account number:

```java
public boolean addAccount(Account account) {
    Account previous = accountsMap.put(account.getAccountNumber(), account);
    return previous == null;
}
```

**Trade-off accepted:** `HashMap` gives no iteration-order guarantee. `viewAllAccounts` and
`getAllAccounts` may list accounts in a different order than they were created. Nothing in the
project specification requires a particular listing order, so this was accepted rather than
paying for a `LinkedHashMap`'s extra per-entry bookkeeping; if creation order ever became a
requirement, changing `new HashMap<>()` to `new LinkedHashMap<>()` is the only line that would
need to change, since the field is typed to the `Map` interface everywhere else.

## `TransactionManager`: `List<Transaction>` (`ArrayList`)

```java
private List<Transaction> transactions = new ArrayList<>();
```

Transactions have no equivalent natural unique key to look up by, one account legitimately has
many, and the dominant operations are "append the next one" and "iterate or filter across all
of them." A `List` is the direct fit: ordered, growable, and permits duplicates (in the sense
of many entries sharing the same account number) without complaint. `addTransaction` therefore
always succeeds, `ArrayList` has no capacity ceiling to check against, unlike `addAccount`, its
boolean return value is now unconditional.

Methods that need "an account's transactions, newest first" (`getTransactionsForAccount`,
`buildTransactionRows`) share one private helper, filtering by account number with a stream and
then reversing the result, since the list's natural order is insertion order (oldest first):

```java
private List<Transaction> newestFirstForAccount(String accountNumber) {
    List<Transaction> matches = transactions.stream()
            .filter(t -> t.getAccountNumber().equals(accountNumber))
            .collect(Collectors.toList());
    Collections.reverse(matches);
    return matches;
}
```

## Streams layered on top

Every filtering, aggregation, and sorting operation in both classes is expressed as a stream
pipeline rather than a hand-written loop with a mutable accumulator. A representative example,
chosen because Test Scenario 8 in the project specification names the technique explicitly:

```java
public double calculateTotalDeposits(String accountNumber) {
    return this.transactions.stream()
            .filter(t -> t.getAccountNumber().equals(accountNumber) && t.getType().equalsIgnoreCase("deposit"))
            .mapToDouble(Transaction::getAmount)
            .reduce(0.0, Double::sum);
}
```

Sorting is built the same way, using `Comparator.comparing`/`comparingDouble` with a method
reference rather than a hand-written `compare` implementation:

```java
public List<Transaction> getTransactionsSortedByAmount(boolean descending) {
    Comparator<Transaction> byAmount = Comparator.comparingDouble(Transaction::getAmount);
    return transactions.stream()
            .sorted(descending ? byAmount.reversed() : byAmount)
            .collect(Collectors.toList());
}
```

`getTransactionsSortedByDate` compares on `Transaction.getCreatedAt()`, a `LocalDateTime`
retained specifically for this purpose. The account's own display timestamp,
`Transaction.getTimestamp()`, is precise only to the minute (`dd-MM-yyyy hh:mm a`), which
proved, when actually tested with several transactions created less than a minute apart, to be
too coarse to sort correctly, ties compared equal and left the list unsorted. Retaining the
underlying `LocalDateTime` (nanosecond precision) alongside the display string, and sorting on
that instead, was the fix; the display string's format is unchanged.

## How persistence and concurrency build on these collections

- **`FilePersistenceService`** needs to iterate every stored account or transaction to save
  them, which neither class exposed before Phase Two. `getAllAccounts()` and
  `getAllTransactions()` both return a defensive copy (`new ArrayList<>(...)`), not the
  internal collection itself or a live view over it, so handing the result to
  `FilePersistenceService` cannot let external code mutate `AccountManager`'s or
  `TransactionManager`'s internal state by surprise.
- **Reloading** discards prior in-memory state first (`AccountManager.clear()` /
  `TransactionManager.clear()`) before loading. Without this, reloading into an
  already-populated `TransactionManager` would append every persisted transaction a second
  time, since `ArrayList.add` has no concept of "this already exists."
- **`ConcurrencyUtils`** calls `TransactionManager.addTransaction` from multiple threads at
  once, `ArrayList` itself is not thread-safe, so `addTransaction` is `synchronized`. This is a
  second, independent race condition from the one `Account.deposit`/`withdraw` guards against,
  discovered only once the concurrent simulation was actually wired to record real
  transactions rather than only touching balances.

## Summary of design decisions

| Decision | Reason |
|---|---|
| `HashMap<String, Account>`, not `ArrayList<Account>` | Accounts have a natural unique key; O(1) lookup replaces O(n) scan |
| `ArrayList<Transaction>`, not a `Map` | Transactions have no unique lookup key and legitimately repeat per account |
| Defensive copies from `getAllAccounts`/`getAllTransactions` | Prevents external code from mutating internal state through the returned collection |
| `clear()` before reload | Prevents `ArrayList.add`'s unconditional append from duplicating transactions on reload |
| Retained `LocalDateTime`, not just the display string | The display format's minute precision was empirically too coarse to sort correctly |
| `synchronized` on `addTransaction`, separate from `Account`'s locks | Guards a second, independent shared mutable structure once concurrent code began writing to it |
