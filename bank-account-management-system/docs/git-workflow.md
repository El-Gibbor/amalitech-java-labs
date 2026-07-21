# Git Workflow

This document records the branch, commit, and cherry-pick history I used to build Phase Two
of the Bank Account Management System, as required by the project specification.

## Branch strategy

I used four branches, each holding a distinct piece of the work:

- `main`: the Phase One codebase, unmodified since Phase Two began. It holds a single commit,
  `Project init: Initial refactoring for clean code`.
- `feature/refactor`: created from `main`. It holds the clean code refactor of the existing
  classes, consisting of helper method extraction, JavaDoc, and the shared `TableFormatter`
  utility.
- `feature/exceptions`: created from `main`, not from `feature/refactor`, matching the
  specification's own git example. It therefore started from the pre-refactor state of
  `Account`, `Customer`, and the manager classes. It holds the custom exception handling
  implementation, and, once that was complete, all three of `feature/refactor`'s commits
  brought across by cherry-pick, described below, followed by the new `StatementGenerator`
  class and the console menu restructuring.
- `feature/testing`: created from `feature/exceptions`. It holds the Maven migration, the
  package restructuring, `AccountManager.transfer`, the JUnit 5 test suite, and the console's
  `Run Tests` wiring.

## Cherry-picking `feature/refactor` into `feature/exceptions`

I deliberately branched `feature/exceptions` from the pre-refactor state of `main`, so that
integrating `feature/refactor`'s commits into it later would be a genuine, necessary operation
rather than a contrived exercise. I originally planned that integration to happen much later,
alongside the remaining version control work, but moved it earlier once it became clear that
the new `StatementGenerator` class would itself depend on the `TableFormatter` utility and the
JavaDoc and clean code conventions `feature/refactor` had already established. Writing the
statement generator against the pre-refactor codebase would have meant a third duplicate of
the table formatting logic, or rework later, so I brought the refactor across first to avoid
both.

I cherry-picked all three `feature/refactor` commits onto `feature/exceptions`, in their
original order:

```
git checkout feature/exceptions
git cherry-pick dd03dff   # JavaDoc: refactor Account class with shared amount validation, remove broken Demo.java
git cherry-pick 16f86c4   # Refactor Customer and Account subtype hierarchies
git cherry-pick 253ccc4   # Extract shared TableFormatter utility; modularize AccountManager and TransactionManager listings
```

Each cherry-pick produced real merge conflicts, since both branches had independently modified
the same methods for different reasons: `feature/refactor` for structure, and
`feature/exceptions` for exception handling. I resolved all of them by hand, keeping the
throw-based method bodies from `feature/exceptions` while adopting the extracted helper
methods from `feature/refactor`.

- `Account.java`: a conflict in `deposit` and `withdraw`. Resolved by keeping the
  exception-throwing bodies and adopting the `isValidAmount` helper extraction, so validation
  reads `if (!isValidAmount(amount)) throw new InvalidAmountException(...)`.
- `SavingsAccount.java` and `CheckingAccount.java`: a conflict in each `withdraw` override.
  Resolved the same way, keeping the exception-throwing bodies and adopting the named private
  helpers, `wouldBreachMinimumBalance` and `exceedsOverdraftLimit`, in place of the inline
  conditions.
- `AccountManager.java`: a conflict limited to `findAccount`'s signature and comment, one side
  returning `null` for a missing account and the other throwing `InvalidAccountException`. The
  throwing version was kept, since it is the correct behavior now that exception handling had
  already been introduced.

`TransactionManager.java` and the new `TableFormatter.java` applied without conflict. The build
was verified after each individual conflict resolution, not only after all three commits had
landed.

## Commit conventions followed throughout

- One concern per commit, whether a single class, a single method, or a single coherent
  feature, matching the granularity visible in `git log`.
- Files staged individually by path, using `git add <path>`, never `git add -A` or
  `git add .`, so that unrelated scratch or untracked files never leak into a feature branch's
  history.
- Commit messages are a single descriptive line. No co-author trailer is appended, by explicit
  project convention.

## Current branch state

```
main               1 commit    Phase One baseline
feature/refactor    3 commits  branched from main
feature/exceptions  9 commits  branched from main, including the 3 cherry-picked refactor commits
feature/testing    14 commits  branched from feature/exceptions: Maven and JUnit setup, transfer, tests, Run Tests
```

`feature/testing` is the current, most complete branch, and the one I made these commits on.
It is merged into `main` through a pull request here on GitHub, `feature/testing` into `main`,
rather than with a local `git merge`, so the final integration is visible and reviewable on
GitHub as part of the project's history.
