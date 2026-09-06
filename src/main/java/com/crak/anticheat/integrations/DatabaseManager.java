package com.crak.anticheat.integrations;

import com.crak.anticheat.Main;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final Main plugin;
    private Connection connection;
    private final String type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfig().getString("database.type", "sqlite");
        this.host = plugin.getConfig().getString("database.host", "localhost");
        this.port = plugin.getConfig().getInt("database.port", 3306);
        this.database = plugin.getConfig().getString("database.database", "anticheat");
        this.username = plugin.getConfig().getString("database.username", "root");
        this.password = plugin.getConfig().getString("database.password", "");
        
        connect();
        createTables();
    }

    private void connect() {
        try {
            if (type.equalsIgnoreCase("mysql")) {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + 
                            "?useSSL=false&autoReconnect=true";
                connection = DriverManager.getConnection(url, username, password);
            } else {
                Class.forName("org.sqlite.JDBC");
                String url = "jdbc:sqlite:plugins/CrakAntiCheat/database.db";
                connection = DriverManager.getConnection(url);
            }
            plugin.getLogger().info("Database connection established!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Players table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16)," +
                "ip VARCHAR(45)," +
                "first_joined TIMESTAMP," +
                "last_joined TIMESTAMP," +
                "banned BOOLEAN DEFAULT FALSE," +
                "ban_reason TEXT," +
                "ban_expiry TIMESTAMP" +
                ")"
            );
            
            // Violations table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS violations (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                "uuid VARCHAR(36)," +
                "check_name VARCHAR(50)," +
                "violations INT," +
                "timestamp TIMESTAMP," +
                "FOREIGN KEY (uuid) REFERENCES players(uuid)" +
                ")"
            );
            
            // Bans table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS bans (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                "uuid VARCHAR(36)," +
                "reason TEXT," +
                "banned_by VARCHAR(50)," +
                "ban_time TIMESTAMP," +
                "expiry_time TIMESTAMP," +
                "active BOOLEAN DEFAULT TRUE," +
                "FOREIGN KEY (uuid) REFERENCES players(uuid)" +
                ")"
            );
            
            // Alts table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS alts (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                "ip VARCHAR(45)," +
                "uuid VARCHAR(36)," +
                "detected TIMESTAMP," +
                "FOREIGN KEY (uuid) REFERENCES players(uuid)" +
                ")"
            );
            
            plugin.getLogger().info("Database tables created/verified!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    public void savePlayer(Player player) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO players (uuid, name, ip, first_joined, last_joined) " +
                "VALUES (?, ?, ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE name = ?, ip = ?, last_joined = NOW()"
            )) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setString(3, player.getAddress().getAddress().getHostAddress());
                stmt.setString(4, player.getName());
                stmt.setString(5, player.getAddress().getAddress().getHostAddress());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save player: " + e.getMessage());
            }
        });
    }

    public void saveViolation(Player player, String check, int violations) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO violations (uuid, check_name, violations, timestamp) VALUES (?, ?, ?, NOW())"
            )) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, check);
                stmt.setInt(3, violations);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save violation: " + e.getMessage());
            }
        });
    }

    public void saveBan(Player player, String reason, String bannedBy, long expiry) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO bans (uuid, reason, banned_by, ban_time, expiry_time) " +
                "VALUES (?, ?, ?, NOW(), ?)"
            )) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, reason);
                stmt.setString(3, bannedBy);
                stmt.setTimestamp(4, new Timestamp(expiry));
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save ban: " + e.getMessage());
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close database: " + e.getMessage());
        }
    }
}