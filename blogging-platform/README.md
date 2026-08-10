
This is the data layer and persistence stage of a blogging platform. It is a JavaFX
desktop application backed by PostgreSQL, reached through plain JDBC, no ORM. It manages
Posts and Comments, with searching, sorting, caching, and a measured performance report
included as evidence of the optimizations applied.

## Prerequisites

* Java 11
* Maven
* PostgreSQL, a recent version. I built and verified this against PostgreSQL 18, but
  nothing here depends on a feature newer than PostgreSQL 10, which is where
  `GENERATED ALWAYS AS IDENTITY` was introduced.

If you are on WSL and plan to run the JavaFX window itself rather than only the
database layer, make sure WSLg is actually rendering windows before troubleshooting this
project specifically. I once lost time to a JavaFX window that registered in the taskbar
but never painted anything, and the fix was `wsl --update` followed by `wsl --shutdown`
from Windows, unrelated to this codebase.

## Database setup

Create an empty database, then load the schema and the sample data in that order.

```
createdb smart_blog
psql -d smart_blog -f db/schema.sql
psql -d smart_blog -f db/seed.sql
```

`db/schema.sql` creates every table, key, constraint, and index. `db/seed.sql` adds
representative sample data, five users, a mix of published and draft posts, tags, post
to tag assignments, comments, and reviews, enough to exercise every feature without
needing to type in your own data first.

## Application configuration

Copy the example properties file and fill in (edit `db.properties` with your own username and password) your own local PostgreSQL credentials.

```
cp src/main/resources/db.properties.example src/main/resources/db.properties
```

## Running the application

```
mvn clean javafx:run
```

This opens the Posts screen. Posts are listed with pagination, a search bar lets you
search by title or content, by author, or by tag, all case insensitive, and a sort
control lets you reorder the full matching result set by title or by published date. My
own new or hand written merge sort algorithm does the sorting once a non default option
is chosen, `util.MergeSort`. Selecting a post shows its comments in a panel on the right,
where you can add, edit, and delete comments on that post.

There is no login screen in this stage of the project. Anything created through the
application, a new post or a new comment, is attributed to a fixed seeded user, since
there is no session to attribute it to instead. This is a scope decision, not an
oversight.



The application follows a Controller, Service, DAO layering throughout. A Controller
never imports `java.sql`, a Service never imports JavaFX, and a DAO never contains
business rules, each layer only knows about the one directly below it.

## Documentation

* `doc/database-design.md` covers the conceptual, logical, and physical models, the
  reasoning behind Reviews existing as its own entity rather than a renamed Comment, the
  constraints a diagram cannot show on its own, and the indexing and referential
  integrity decisions.
* `doc/performance-report.md` measures caching and indexing against a real database,
  including a case where an index did not help and an explanation of why, rather than
  only reporting the numbers that looked good.

## Reproducing the performance numbers

`bench.PerformanceBenchmark` produced every number in the performance report. It bulk
inserts synthetic posts and drops and recreates an index inside one transaction, then
rolls that transaction back, so running it does not change your actual data.

```
mvn -q compile
java -cp target/classes:$(find ~/.m2 -name 'postgresql-42.7.4.jar' | head -1) bench.PerformanceBenchmark
```

## What is not built yet

* Users, Tags, and Reviews have a schema, constraints, and seed data, but no JavaFX
  screen or CRUD code. Only Posts and Comments are wired all the way through, since
  those are the two entities the specification names explicitly for CRUD through the
  interface.
* Reviews is the entity chosen for the NoSQL document schema comparison, a design and
  justification deliverable, not an application feature, so its absence from the
  interface does not leave that requirement unmet.
