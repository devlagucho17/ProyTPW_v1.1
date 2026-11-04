package subastix.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public final class Database {
    private static volatile HikariDataSource dataSource;

    private Database() {
    }

    public static DataSource getDataSource() {
        HikariDataSource current = dataSource;
        if (current != null) return current;
        synchronized (Database.class) {
            if (dataSource == null) {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(environment("DB_URL",
                        "jdbc:mysql://localhost:3306/subastix_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC"));
                config.setUsername(environment("DB_USER", "subastix"));
                config.setPassword(environment("DB_PASSWORD", ""));
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setConnectionTimeout(10000);
                config.setPoolName("SubastIX-DB");
                dataSource = new HikariDataSource(config);
            }
            return dataSource;
        }
    }

    public static void close() {
        HikariDataSource current = dataSource;
        if (current != null) {
            current.close();
            dataSource = null;
        }
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
