package de.mcterranova.playerActionAdapter.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.mcterranova.playerActionAdapter.PlayerActionAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class HikariCP {

    //static Dotenv secret;

    private final PlayerActionAdapter plugin;
    private final String user;
    private final String password;
    public HikariDataSource dataSource;

    public HikariCP(PlayerActionAdapter plugin) throws SQLException {


        this.plugin = plugin;
        //secret = Dotenv.configure().directory().filename(".env").load();

        //user = secret.get("USERNAME");
        user = "minecraft";
        //System.out.println(user);
        //password = secret.get("PASSWORD");
        password = "minecraft";
        //System.out.println(password);

        HikariConfig config = getHikariConfig();
        dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Driver Name: " + metaData.getDriverName());
            System.out.println("Driver Version: " + metaData.getDriverVersion());
            System.out.println("Database Product Name: " + metaData.getDatabaseProductName());
            System.out.println("Database Product Version: " + metaData.getDatabaseProductVersion());
        }
    }

    private @NotNull HikariConfig getHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost/nations");
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(20);
        config.setMaxLifetime(1800000);
        config.setKeepaliveTime(0);
        config.setConnectionTimeout(5000);
        config.setLeakDetectionThreshold(100000);
        config.setPoolName("NationsHikariPool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        //before true
        config.addDataSourceProperty("useServerPrepStmts", "false");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("useLocalTransactionState", "true");
        //before true
        config.addDataSourceProperty("rewriteBatchedStatements", "false");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        return config;
    }

    public void closeConnection() throws SQLException {
        dataSource.close();
    }

}
