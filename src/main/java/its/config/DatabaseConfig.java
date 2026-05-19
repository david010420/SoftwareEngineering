package its.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {

    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {}

    /** Double-checked locking 싱글턴 */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    dataSource = build();
                }
            }
        }
        return dataSource;
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static HikariDataSource build() {
        Properties props = loadProperties();

        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        cfg.setJdbcUrl(props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/its_db" +
                        "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8"));
        cfg.setUsername(props.getProperty("db.username", "its_user"));
        cfg.setPassword(props.getProperty("db.password", "its_password"));
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(30_000);
        cfg.setIdleTimeout(600_000);
        cfg.setMaxLifetime(1_800_000);
        cfg.setPoolName("ITS-MySQL-Pool");

        return new HikariDataSource(cfg);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConfig.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            System.err.println("[WARN] db.properties not found, using defaults.");
        }
        return props;
    }
}
