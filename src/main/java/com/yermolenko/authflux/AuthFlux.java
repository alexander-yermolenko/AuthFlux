package com.yermolenko.authflux;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

public final class AuthFlux extends JavaPlugin {
    private DatabaseManager databaseManager;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        getLogger().info("AuthFlux has been enabled!");

        // Save default config.yml and messages.yml if they don't exist
        saveDefaultConfig();
        loadMessagesConfig();

        // Initialize database
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initializeDatabase();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands and listeners
        AuthCommandExecutor commandExecutor = new AuthCommandExecutor(this);
        Objects.requireNonNull(getCommand("reg")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("log")).setExecutor(commandExecutor);
        getServer().getPluginManager().registerEvents(new AuthListener(this), this);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("AuthFlux has been disabled!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
}
