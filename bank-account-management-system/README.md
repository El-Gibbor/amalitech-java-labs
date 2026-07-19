## Features

| Feature | Description |
|---|---|
| **Create Account** | Register a new account for a Regular or Premium customer |
| **View Accounts** | List all accounts with balances, plus total accounts and total bank balance |
| **Process Transaction** | Deposit or withdraw money, with a confirmation step before finalizing |
| **View Transaction History** | Show an account's transactions (newest first) with deposit/withdrawal/net-change totals |
| **Menu Navigation** | Simple looping menu that keeps running until the user exits |

The application starts with five seeded demo accounts (3 Savings, 2 Checking)
so the listing has data on first launch.

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

---

## Project Structure

All classes live in a single package (flat directory).

| Class | Role |
|---|---|
| `Account` (abstract) | Base account: number, customer, balance, status, deposit/withdraw; implements `Transactable` |
| `SavingsAccount` | Adds interest rate and minimum-balance rule on withdrawals |
| `CheckingAccount` | Adds overdraft limit and monthly fee (waived for Premium) |
| `Customer` (abstract) | Base customer: id, name, age, contact, address |
| `RegularCustomer` | Standard customer |
| `PremiumCustomer` | Premium customer with waived fees |
| `Transactable` (interface) | Contract for `processTransaction(amount, type)` |
| `Transaction` | Immutable record of one deposit/withdrawal with auto-generated ID and timestamp |
| `AccountManager` | Holds accounts in an array; add, linear-search find, list, totals |
| `TransactionManager` | Holds transactions in an array; add, per-account view and totals |
| `Main` | Console menu and program flow |

Unique IDs are generated with static counters: accounts as `ACC001`,
customers as `CUS001`, and transactions as `TXN001`.

---

## Requirements

- Java Development Kit (JDK) 11 or later (`String.repeat` is used).

---

## Build and Run

The console UI uses box-drawing characters (`╔ ═ ─`), which require a UTF-8
environment to display correctly.

> **⚠️ Do not use Windows PowerShell.** PowerShell renders the box-drawing
> characters as `?????` regardless of code page or JVM flags. Use one of the
> supported options below instead.

### Recommended: run from your IDE

The simplest way, with no encoding flags needed:

- **IntelliJ IDEA**: open the project, open `Main.java`, and click the
  **Run** button (▶)
- **VS Code** (with the Java Extension Pack): open `Main.java` and click
  **Run** abutton (▶)

Both compile and run with UTF-8 automatically.

### Command line: Git Bash or a Unix-like shell

Use **Git Bash** on Windows, or any Linux/macOS terminal (Ubuntu, WSL, etc.).
From this directory:

```bash
# Compile with UTF-8
javac -encoding UTF-8 *.java

# Run with UTF-8 output
java -Dfile.encoding=UTF-8 Main
```

These terminals display UTF-8 correctly, so the menu and tables render as
intended.

---

## Usage

At the main menu, enter the number of the option you want:

```
1. Create Account
2. View All Accounts
3. Process Transaction
4. View Transactions history
5. Exit
```

- **Create Account**: enter the customer's name, age, contact, and address,
  then choose the customer type (Regular/Premium), account type
  (Savings/Checking), and an initial deposit.
- **Process Transaction**: enter an account number, choose Deposit or
  Withdrawal, enter an amount, then confirm with `Y` to finalize (or `N` to
  cancel; a cancelled transaction leaves the balance unchanged).
- **View Transaction History**: enter an account number to see its
  transactions and summary totals.

Invalid input is handled gracefully: non-numeric menu choices and amounts are
rejected and re-prompted instead of crashing, amounts must be greater than
zero, savings withdrawals cannot breach the $500 minimum balance, and checking
withdrawals cannot exceed the overdraft limit.
