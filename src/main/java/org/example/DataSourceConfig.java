package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceConfig {
    private static DataSource dataSource;

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void initialize(String dbServerUrl, String dbUser, String dbPass) {
//        HikariConfig config = new HikariConfig();
//        config.setJdbcUrl(dbServerUrl);
//        config.setUsername(dbUser);
//        config.setPassword(dbPass);
//        config.setDriverClassName("org.postgresql.Driver");
//        config.setMaximumPoolSize(10); // Максимальное количество соединений в пуле
//        dataSource = new HikariDataSource(config);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbServerUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPass);
        config.setDriverClassName("org.postgresql.Driver");

        // Настройки пула соединений
        config.setMaximumPoolSize(10); // Максимальное количество соединений в пуле
        config.setMinimumIdle(2); // Минимальное количество соединений, которые будут поддерживаться в пуле
        config.setIdleTimeout(30000); // Время в миллисекундах, после которого неиспользуемое соединение будет закрыто (30 секунд)
        config.setMaxLifetime(1800000); // Максимальное время жизни соединения в миллисекундах (30 минут)
        config.setConnectionTimeout(30000); // Максимальное время ожидания получения соединения из пула (30 секунд)

        // Проверка работоспособности соединений
        config.setConnectionTestQuery("SELECT 1"); // Запрос для проверки работоспособности соединения
        config.setValidationTimeout(5000); // Максимальное время на выполнение проверки соединения (5 секунд)

        // Дополнительные настройки
        config.addDataSourceProperty("cachePrepStmts", "true"); // Включение кэширования подготовленных запросов
        config.addDataSourceProperty("prepStmtCacheSize", "250"); // Размер кэша для подготовленных запросов
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); // Максимальный размер SQL-запроса в кэше

        dataSource = new HikariDataSource(config);
    }


}