package bench;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import dao.PostDao;
import service.PostService;
import util.DatabaseConnection;

/**
 * Produces the timing evidence quoted in doc/performance-report.md. The
 * indexing test rolls back its own transaction, so it leaves the database
 * unchanged and is safe to rerun.
 */
public final class PerformanceBenchmark {
    private static final int SYNTHETIC_POST_COUNT = 20_000;

    // Unique across all synthetic posts, so this matches exactly one row.
    private static final String TARGET_TITLE = "Synthetic post 10000";

    // High warmup count avoids JIT bias between the with and without index phases.
    private static final int INDEX_WARMUP_RUNS = 300;
    private static final int INDEX_TIMED_RUNS = 100;

    public static void main(String[] args) throws Exception {
        benchmarkCaching();

        try (Connection connection = DatabaseConnection.get()) {
            connection.setAutoCommit(false);
            try {
                seedSyntheticPosts(connection);
                benchmarkIndexing(connection);
            } finally {
                connection.rollback();
                System.out.println();
                System.out.println("Transaction rolled back, database left unchanged.");
            }
        }
    }

    private static void benchmarkCaching() {
        PostService service = new PostService(new PostDao());

        // Warms up the JVM and connection on a different page before timing.
        service.listPosts(2, 5);

        long missNanos = timeCall(() -> service.listPosts(1, 5));
        long hitNanos = timeCall(() -> service.listPosts(1, 5));

        System.out.println("=== Caching, listPosts(1, 5) ===");
        System.out.printf("First call  (cache miss, hits the database): %.3f ms%n", millis(missNanos));
        System.out.printf("Second call (cache hit, served from memory): %.3f ms%n", millis(hitNanos));
        System.out.printf("Speedup: %.1fx%n", (double) missNanos / hitNanos);
        System.out.printf("Hits recorded: %d, misses recorded: %d%n", service.getCacheHits(), service.getCacheMisses());
    }

    private static void seedSyntheticPosts(Connection connection) throws Exception {
        System.out.println();
        System.out.println("Inserting " + SYNTHETIC_POST_COUNT + " synthetic posts for a realistic table size...");
        String sql = "INSERT INTO posts (user_id, title, content) "
                + "SELECT (n % 5) + 1, 'Synthetic post ' || n, 'Benchmark content' "
                + "FROM generate_series(1, ?) AS n";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, SYNTHETIC_POST_COUNT);
            statement.executeUpdate();
        }
        try (Statement statement = connection.createStatement()) {
            // Refreshes planner statistics to account for the rows just inserted.
            statement.execute("ANALYZE posts");
        }
    }

    private static void benchmarkIndexing(Connection connection) throws Exception {
        String sql = "SELECT post_id, title FROM posts WHERE title = ?";

        System.out.println();
        System.out.println("=== Indexing, SELECT ... WHERE title = ? (one matching row out of "
                + SYNTHETIC_POST_COUNT + ") ===");
        System.out.println("-- With idx_posts_title --");
        double withIndexMs = averageQueryTime(connection, sql, INDEX_WARMUP_RUNS, INDEX_TIMED_RUNS);
        explain(connection, sql);

        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP INDEX idx_posts_title");
        }

        System.out.println();
        System.out.println("-- Without idx_posts_title --");
        double withoutIndexMs = averageQueryTime(connection, sql, INDEX_WARMUP_RUNS, INDEX_TIMED_RUNS);
        explain(connection, sql);

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX idx_posts_title ON posts(title)");
        }

        System.out.println();
        System.out.printf("Average with index:    %.3f ms%n", withIndexMs);
        System.out.printf("Average without index: %.3f ms%n", withoutIndexMs);
        System.out.printf("Speedup: %.1fx%n", withoutIndexMs / withIndexMs);
    }

    // Reuses one prepared statement so parse and plan cost is paid once, not per call.
    private static double averageQueryTime(Connection connection, String sql, int warmupRuns, int timedRuns)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TARGET_TITLE);
            long totalNanos = 0;
            for (int i = 0; i < warmupRuns + timedRuns; i++) {
                long start = System.nanoTime();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        // drains the result set so timing includes fetching, not just planning
                    }
                }
                long elapsed = System.nanoTime() - start;
                if (i >= warmupRuns) {
                    totalNanos += elapsed;
                }
            }
            return millis(totalNanos / timedRuns);
        }
    }

    private static void explain(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("EXPLAIN ANALYZE " + sql)) {
            statement.setString(1, TARGET_TITLE);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    System.out.println(resultSet.getString(1));
                }
            }
        }
    }

    private static long timeCall(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return System.nanoTime() - start;
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
