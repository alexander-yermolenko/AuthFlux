package com.yermolenko.authflux;

import org.bukkit.Location;
import org.bukkit.World;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Objects;
import java.util.logging.Level;

public class DatabaseManager {
    private final AuthFlux plugin;
    private Connection connection;

    public DatabaseManager(AuthFlux plugin) {
        this.plugin = plugin;
    }

    public void initializeDatabase() throws SQLException {
        // Explicitly load the PostgreSQL driver
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC driver not found", e);
        }

        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 5432);
        String database = plugin.getConfig().getString("database.name", "authflux");
        String username = plugin.getConfig().getString("database.username", "postgres");
        String password = plugin.getConfig().getString("database.password", "password");
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);

        try {
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("Successfully connected to PostgreSQL database");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to PostgreSQL database: " + e.getMessage(), e);
            throw e;
        }

        createTables();
    }

    private void createTables() throws SQLException {
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    password VARCHAR(60) NOT NULL,
                    is_logged_in BOOLEAN DEFAULT FALSE,
                    world VARCHAR(255),
                    x DOUBLE PRECISION,
                    y DOUBLE PRECISION,
                    z DOUBLE PRECISION,
                    yaw FLOAT,
                    pitch FLOAT
                );
                """;
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.execute();
            plugin.getLogger().info("Players table created or verified");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create players table: " + e.getMessage(), e);
            throw e;
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to close database connection: " + e.getMessage(), e);
            }
        }
    }

    public boolean isPlayerRegistered(String uuid) {
        String sql = "SELECT 1 FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking player registration: " + e.getMessage(), e);
            return false;
        }
    }

    public void registerPlayer(String uuid, String username, String password) {
        String sql = "INSERT INTO players (uuid, username, password) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setString(2, username);
            stmt.setString(3, BCrypt.hashpw(password, BCrypt.gensalt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error registering player: " + e.getMessage(), e);
        }
    }

    public boolean checkPlayerPassword(String uuid, String password) {
        String sql = "SELECT password FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                return BCrypt.checkpw(password, storedHash);
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking player password: " + e.getMessage(), e);
            return false;
        }
    }

    public void setPlayerLoggedIn(String uuid, boolean loggedIn) {
        String sql = "UPDATE players SET is_logged_in = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, loggedIn);
            stmt.setString(2, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating player login status: " + e.getMessage(), e);
        }
    }

    public boolean isPlayerLoggedIn(String uuid) {
        String sql = "SELECT is_logged_in FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_logged_in");
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking player login status: " + e.getMessage(), e);
            return false;
        }
    }

    public void savePlayerLocation(String uuid, Location location) {
        String sql = """
                UPDATE players SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?
                WHERE uuid = ?
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, Objects.requireNonNull(location.getWorld()).getName());
            stmt.setDouble(2, location.getX());
            stmt.setDouble(3, location.getY());
            stmt.setDouble(4, location.getZ());
            stmt.setFloat(5, location.getYaw());
            stmt.setFloat(6, location.getPitch());
            stmt.setString(7, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving player location: " + e.getMessage(), e);
        }
    }

    public Location getPlayerLocation(String uuid) {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String worldName = rs.getString("world");
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    return null;
                }
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");
                return new Location(world, x, y, z, yaw, pitch);
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error retrieving player location: " + e.getMessage(), e);
            return null;
        }
    }
}
