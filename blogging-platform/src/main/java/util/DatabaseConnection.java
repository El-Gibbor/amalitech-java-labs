package util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens a JDBC connection to PostgreSQL using settings read from
 * db.properties on the classpath. 
 */
public final class DatabaseConnection {
    private static final Properties SETTINGS = loadSettings();

    private DatabaseConnection() {
    }

    private static Properties loadSettings() {
        Properties properties = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties not found on the classpath. Copy db.properties.example "
                                + "to db.properties and fill in your local credentials.");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read db.properties", e);
        }
        return properties;
    }

    /** @return a new JDBC connection; the caller is responsible for closing it. */
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(
                SETTINGS.getProperty("db.url"),
                SETTINGS.getProperty("db.user"),
                SETTINGS.getProperty("db.password"));
    }
}
