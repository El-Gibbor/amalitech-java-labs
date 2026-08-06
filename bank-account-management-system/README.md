## Features

| Feature | Description |
|---|---|
| **Create Account** | Register a new account for a Regular or Premium customer, with contact, email, and account number formats validated by regex before the account is created |
| **View Accounts** | List all accounts with balances, plus total accounts and total bank balance |
| **Process Transaction** | Deposit or withdraw money, with a confirmation step before finalizing |
| **View Transaction History** | Show an account's transactions (newest first) with total deposits, total withdrawals, and net change |
| **View All Transactions (Sorted)** | List every recorded transaction across all accounts, sorted by amount or by date, using the Streams API |
| **Generate Account Statement** | A shorter, statement-style summary of an account's transactions and net change |
| **Save/Load Data** | Save all accounts and transactions to disk on demand, or discard in-memory state and reload it from the saved files |
| **Run Concurrent Simulation** | Run three deposits and withdrawals against one account on separate threads at the same time, demonstrating thread-safe balance updates |
| **Run Tests** | Execute the JUnit 5 test suite from within the running application and report how many tests passed and how many failed |
| **Menu Navigation** | Looping menu, organized into Manage Accounts and Perform Transactions submenus, that keeps running until the user exits |

Accounts and transactions persist automatically between runs: the application loads
`data/accounts.txt` and `data/transactions.txt` on startup and saves both on exit. The five
seeded demo accounts (3 Savings, 2 Checking) are only created on a genuine first run, when
nothing was loaded from disk.

### Account Types

| Type | Details |
|---|---|
| **Savings** | Interest rate 3.5% annually, minimum balance $500 (enforced on withdrawal) |
| **Checking** | No interest, overdraft limit $1,000, monthly fee $10 |

### Customer Types

| Type | Details |
|---|---|
| **Regular** | Standard banking services |
| **Premium** | Minimum balance $10,000, monthly fees waived |

## Exception Handling

Invalid conditions are reported through four custom checked exceptions rather than boolean
return values or silent failures. Each is caught in `Main` and displayed as a clear console
message instead of crashing the application. These govern *business rule* failures, an
account that legitimately does not exist, an amount that legitimately cannot be withdrawn,
which are distinct from the *format* failures `ValidationUtils` catches earlier, at the point
of input (see [Regex Validation](#regex-validation) below).

| Exception | Thrown when |
|---|---|
| `InvalidAmountException` | A deposit, withdrawal, or transfer amount is not greater than zero |
| `InsufficientFundsException` | A withdrawal would exceed the balance, or (for a savings account) breach the $500 minimum balance |
| `OverdraftExceededException` | A checking account withdrawal would exceed the $1,000 overdraft limit |
| `InvalidAccountException` | An account number does not match any stored account, including a transfer whose source and destination are the same account |

## Collections and Functional Programming

`AccountManager` and `TransactionManager` are backed by `HashMap<String, Account>` and
`List<Transaction>` (an `ArrayList`) respectively, replacing the fixed-size arrays used in
earlier phases. A `HashMap` keyed by account number gives average-case O(1) lookup by
`findAccount`, instead of the O(n) linear scan a fixed array required, and neither collection
imposes a maximum capacity.

Filtering, sorting, and aggregation throughout both classes use the Streams API with lambdas
and method references rather than hand-written loops, for example:

```java
public double calculateTotalDeposits(String accountNumber) {
    return this.transactions.stream()
            .filter(t -> t.getAccountNumber().equals(accountNumber) && t.getType().equalsIgnoreCase("deposit"))
            .mapToDouble(Transaction::getAmount)
            .reduce(0.0, Double::sum);
}
```

`TransactionManager.getTransactionsSortedByAmount` and `getTransactionsSortedByDate` build a
`Comparator` with `Comparator.comparing`/`comparingDouble` and a method reference, and expose
the result through the console's **View All Transactions (Sorted)** option. See
[`docs/collections-architecture.md`](docs/collections-architecture.md) for the full design
rationale.

## File Persistence

`services/FilePersistenceService` saves and loads accounts and transactions as plain text
files under `data/`, using the NIO `Files` and `Paths` APIs (`Files.write` to save,
`Files.lines` inside try-with-resources to load, mapping each line to an object via method
references). Each line is pipe-delimited (`|`), chosen over commas because the seed data's
addresses already contain commas.

```
accounts.txt:      accountType|accountNumber|balance|status|customerType|customerId|name|age|contact|email|address
transactions.txt:  transactionId|accountNumber|type|amount|balanceAfter|createdAt(ISO-8601)
```

A missing file is not treated as an error: `Files.exists` is checked first, and if the file is
absent, `FilePersistenceService` starts empty rather than throwing, so deleting `data/accounts.txt`
and relaunching the application recreates it on the next save without crashing.

Loading an account or a customer restores its original ID (`ACC003`, `CUS007`) rather than
generating a new one, and the corresponding ID counters are advanced past every restored value
so that new accounts or customers created afterward in the same session never collide with
data just reloaded from disk.

## Regex Validation

`utils/ValidationUtils` centralizes three compiled `Pattern`s, each exposed as both a
`Predicate<String>` constant and a convenience boolean method:

| Input | Pattern | Example |
|---|---|---|
| Account number | `ACC\d{3}` | `ACC003` |
| Email | `^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+$` | `name@example.com` |
| Phone number | `^\+?[0-9]+(-[0-9]+)*$` | `+1-555-0101` |

The email pattern is tightened slightly from the literal pattern in the project specification,
which does not actually require a dot in the domain and would accept `name@bank` with no top
level domain; requiring at least one `.label` after the `@` matches the specification's own
worked example, where `john.smith@bank` is rejected and only `john.smith@bank.com` is accepted.

These are applied at every relevant console prompt (customer contact and email during account
creation, account number at every point one is entered) with a re-prompt loop and a clear error
message, catching format mistakes before any business object is touched, so
`InvalidAccountException` is reserved purely for a well-formed account number that legitimately
does not exist.

## Concurrency

`Account.deposit` and `Account.withdraw` (and each subclass's `withdraw` override) are
`synchronized`, since both read the current balance, compute a new value, and write it back,
a sequence that is not atomic on its own and can silently lose an update if two threads
execute it on the same account at the same time.

`utils/ConcurrencyUtils` runs a batch of deposits and withdrawals against one account on
separate, named threads (`Thread-1`, `Thread-2`, ...), starting every thread before joining
any of them so they genuinely run concurrently, then prints the verified final balance. Each
operation is also recorded as a `Transaction`; since multiple threads can call
`TransactionManager.addTransaction` at the same time and `ArrayList` is not thread-safe on its
own, that method is `synchronized` as well, and the deposit-or-withdrawal-plus-balance-read
sequence in `ConcurrencyUtils` is wrapped in its own `synchronized (account)` block so the
balance recorded on each `Transaction` is guaranteed to be that operation's own result, not a
later thread's.

Console output interleaves unpredictably between threads, by design, run to run; the final
balance and every transaction's recorded balance are what's guaranteed correct.

## Requirements

- Java Development Kit (JDK) 11 or later
- Apache Maven 3.6 or later

## Build and Run

The console UI uses box-drawing characters (`╔ ═ ─`), which require a UTF-8
environment to display correctly.

> **⚠️ Do not use Windows PowerShell.** PowerShell renders the box-drawing
> characters as `?????` regardless of code page or JVM flags. Use one of the
> supported options below instead.

### Run the application from your IDE

- **IntelliJ IDEA**: open the project (IntelliJ detects `pom.xml` automatically), open
  `Main.java`, and click the **Run** button (▶)
- **VS Code** (with the Java Extension Pack): open the folder and click **Run** above
  `public static void main`

Both resolve dependencies and compile with UTF-8 automatically. This works for every menu
option **except option 6, "Run Tests"**: most IDE run configurations only put the main output on
the classpath, not the compiled test classes, which is what a `ClassNotFoundException` for a
test class at that point means. Use the command below for that option instead.

### Run the application from the command line, including option 6

From this directory:

```bash
mvn test-compile exec:java
```

This compiles both main and test sources, then runs `Main` with the main output, the compiled
test classes, and every dependency on the classpath together, so every menu option, including
"**Run Tests**," works regardless of which IDE, if any, is being used. This is also the most
reliable way to run the application, and the one to use for option 6 specifically.

---

## Testing

The JUnit 5 suite covers valid and invalid cases for `deposit`, `withdraw`, and `transfer`,
including that each custom exception is thrown under the correct condition, plus
`TransactionManager`'s recording and per-account querying of transactions. This suite predates
Phase Three, and every migration in this phase, to `HashMap`/`ArrayList`, to Streams, to file
persistence, to regex validation, to `synchronized`, was verified against it at each step to
confirm it kept passing unmodified. `AccountTest` and `ExceptionTest` each needed one line
changed, adding an email argument to a `RegularCustomer` constructor call, purely to keep
compiling against `Customer`'s new constructor signature; no test's assertions or logic changed.

| Test class | Covers |
|---|---|
| `AccountTest` | `Account.deposit` and `Account.withdraw`, and `AccountManager.transfer`, valid and invalid cases, for both account types |
| `TransactionManagerTest` | Recording transactions, filtering and summing them by account and type, newest-first ordering |
| `ExceptionTest` | The four custom exception classes' own contract, plus exception conditions not otherwise covered |

Run them with `mvn test`, or from within the running application via menu option 6, "Run
Tests," which executes the same suite through the JUnit Platform Launcher and prints a
per-test result line followed by a summary.

Phase Three's own features, Collections migration, File I/O, regex, and concurrency, are
verified against Bank-Account-III.md's nine test scenarios directly (round-trip persistence,
regex acceptance and rejection, concurrent balance and transaction correctness, stream-based
sorting and reduction, and missing-file recovery), rather than by new JUnit tests; none of
those scenarios have a dedicated automated test yet, only the manual verification recorded
during development.

### Test results

As of the current commit, all 22 tests pass, with no failures, errors, or skips:

| Test class | Tests | Passed |
|---|---|---|
| `AccountTest` | 9 | 9 |
| `TransactionManagerTest` | 5 | 5 |
| `ExceptionTest` | 8 | 8 |
| **Total** | **22** | **22** |

Output from `mvn test`, run on `feature/testing`:

![mvn test output showing 22 tests run across TransactionManagerTest, AccountTest, and ExceptionTest, all passing, with BUILD SUCCESS](docs/images/mvn-test-results.png)

Output from the console's own "Run Tests" option, which runs the same suite
through the JUnit Platform Launcher rather than through Maven:

![Console output of the Run Tests menu option, listing each of the 22 tests as PASSED, followed by a summary confirming all 22 tests passed successfully](docs/images/console-test-result.png)

(The order tests print in can vary between runs, since JUnit does not guarantee execution
order across test classes; the pass count does not.)

---

## Git Workflow

Phase Two was built across four branches (`feature/refactor`, `feature/exceptions`,
`feature/testing`, off `main`), including a deliberate `git cherry-pick` of the refactor
branch's commits into the exceptions branch, with real conflicts resolved by hand. See
[`docs/git-workflow.md`](docs/git-workflow.md) for the full branch history, the cherry-pick
commands used, and the reasoning behind each conflict resolution.

Phase Three followed the same one-branch-per-feature pattern, six branches off `main`, each
verified with a full `mvn test` run in its own final state and merged back with a real
(`--no-ff`) merge commit, verified again immediately after each merge:

| Branch | Covers |
|---|---|
| `feature/collections` | `HashMap`/`ArrayList` migration, Streams-based filtering, sorting, and aggregation |
| `feature/file-persistence` | `FilePersistenceService`, the accounts/transactions file format, reconstruction constructors |
| `feature/regex-validation` | `ValidationUtils`, the `Customer` email field, console validation wiring |
| `feature/concurrency` | `synchronized` on `Account`, `ConcurrencyUtils`, the concurrent simulation menu option |
| `feature/save-load-menu` | The on-demand Save/Load Data menu option |
| `feature/reporting-and-docs` | Closing the Test Scenario 5 and 7 gaps found during Sub-phase Five verification, this documentation |

---

## Usage

At the main menu, enter the number of the option you want:

```
1. Manage Accounts
2. Perform Transactions
3. Generate Account Statements
4. Save/Load Data
5. Run Concurrent Simulation
6. Run Tests
7. Exit
```

- **Manage Accounts**, then **Create Account**: enter the customer's name, age, contact,
  email, and address (contact and email are validated against a phone-number and an
  email-address pattern respectively, re-prompting on a bad format), then choose the customer
  type (Regular or Premium), account type (Savings or Checking), and an initial deposit.
- **Manage Accounts**, then **View All Accounts**: list every account with its balance and
  type-specific details.
- **Perform Transactions**, then **Process Transaction**: enter an account number, choose
  Deposit or Withdrawal, enter an amount, then confirm with `Y` to finalize (or `N` to
  cancel; a cancelled transaction leaves the balance unchanged).
- **Perform Transactions**, then **View Transaction History**: enter an account number to see
  its transactions and summary totals.
- **Perform Transactions**, then **View All Transactions (Sorted)**: list every transaction
  across every account, sorted by amount or by date.
- **Generate Account Statements**: enter an account number for a shorter, totals-focused
  summary of its transactions.
- **Save/Load Data**: save immediately on demand, or discard in-memory state and reload from
  the last saved files without restarting the application.
- **Run Concurrent Simulation**: enter an account number to run three concurrent deposits and
  withdrawals against it and observe the interleaved thread activity and verified final
  balance.
- **Run Tests**: runs the JUnit 5 suite from within the application. See
  [Testing](#testing) above for what it covers and current results.

Invalid input is handled gracefully: non-numeric menu choices and amounts, and badly
formatted account numbers, phone numbers, and email addresses are rejected and re-prompted
instead of crashing, and business-rule violations (a non-positive amount, an unknown but
well-formed account number, a savings withdrawal breaching the $500 minimum, a checking
withdrawal exceeding the overdraft limit) are reported as clear error messages rather than
crashing the application.

## Project Structure

```
bank-account-management-system/
├── pom.xml
├── data/
│   ├── accounts.txt
│   └── transactions.txt
├── docs/
│   ├── git-workflow.md
│   └── collections-architecture.md
├── src/
│   ├── main/java/
│   │   ├── Main.java
│   │   ├── models/                  Account, Customer hierarchies, Transaction, Transactable
│   │   │   └── exceptions/          The four custom exceptions
│   │   ├── services/                AccountManager, TransactionManager, StatementGenerator,
│   │   │                            FilePersistenceService
│   │   └── utils/                   TableFormatter, ValidationUtils, ConcurrencyUtils
│   └── test/java/                   Mirrors the package layout above
│       ├── models/AccountTest.java
│       ├── models/exceptions/ExceptionTest.java
│       └── services/TransactionManagerTest.java
```
