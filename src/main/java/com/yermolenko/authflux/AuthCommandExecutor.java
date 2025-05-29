package com.yermolenko.authflux;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class AuthCommandExecutor implements CommandExecutor {
    private final AuthFlux plugin;

    public AuthCommandExecutor(AuthFlux plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("only-players"))));
            return true;
        }

        DatabaseManager dbManager = plugin.getDatabaseManager();
        String uuid = player.getUniqueId().toString();
        int minPasswordLength = plugin.getConfig().getInt("password.min-length", 4);
        int maxPasswordLength = plugin.getConfig().getInt("password.max-length", 24);

        if (command.getName().equalsIgnoreCase("reg")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("reg-usage"))));
                return true;
            }

            String password = args[0];
            if (password.length() < minPasswordLength || password.length() > maxPasswordLength) {
                String message = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("password-length-invalid")))
                        .replace("%min%", String.valueOf(minPasswordLength))
                        .replace("%max%", String.valueOf(maxPasswordLength));
                player.sendMessage(message);
                return true;
            }

            if (dbManager.isPlayerRegistered(uuid)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("reg-already-registered"))));
                return true;
            }

            if (!dbManager.registerPlayer(uuid, player.getName(), password)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cRegistration failed due to a server error. Please try again later."));
                return true;
            }

            // Save initial location as spawn point
            Location initialLocation = loadSpawnPoint(); // Assume this method is accessible or move it here
            if (initialLocation != null) {
                dbManager.savePlayerLocation(uuid, initialLocation);
            } else {
                plugin.getLogger().warning("Failed to load spawn point for " + player.getName());
            }

            if (!dbManager.setPlayerLoggedIn(uuid, true)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cRegistration succeeded, but failed to set login status. Please try logging in."));
                return true;
            }

            unfreezeAndTeleport(player, uuid);
            String successMessage = plugin.getMessagesConfig().getString("reg-success");
            if (successMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cRegistration succeeded, but success message is missing. Contact an admin."));
                plugin.getLogger().warning("reg-success message not found in messages.yml");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("log")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("log-usage"))));
                return true;
            }

            if (!dbManager.isPlayerRegistered(uuid)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("log-not-registered"))));
                return true;
            }

            if (dbManager.isPlayerLoggedIn(uuid)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("log-already-logged-in"))));
                return true;
            }

            if (dbManager.checkPlayerPassword(uuid, args[0])) {
                dbManager.setPlayerLoggedIn(uuid, true);
                unfreezeAndTeleport(player, uuid);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("log-success"))));
                plugin.getLogger().info("Player " + player.getName() + " logged in successfully");
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("log-wrong-password"))));
            }
            return true;
        }

        return false;
    }

    private void unfreezeAndTeleport(Player player, String uuid) {
        // Unfreeze player
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.SLOW);
        plugin.getLogger().info("Unfroze player " + player.getName());

        // Teleport to saved location or spawn point
        Location savedLocation = plugin.getDatabaseManager().getPlayerLocation(uuid);
        Location teleportLocation = savedLocation != null && savedLocation.getWorld() != null ? savedLocation : loadSpawnPoint();
        if (teleportLocation != null && teleportLocation.getWorld() != null) {
            player.teleport(teleportLocation);
            plugin.getLogger().info("Teleported " + player.getName() + " to " + (savedLocation != null ? "saved location" : "spawn point") + ": " + teleportLocation);
        } else {
            plugin.getLogger().warning("No valid teleport location for " + player.getName() + ", keeping player at current position");
        }
    }

    private Location loadSpawnPoint() {
        World world = plugin.getServer().getWorld(plugin.getConfig().getString("spawn-point.world", "world"));
        if (world == null) {
            plugin.getLogger().warning("Spawn world not found! Using default world.");
            world = plugin.getServer().getWorlds().getFirst();
        }
        if (world == null) {
            return null; // Fallback to null if no world is available
        }
        double x = plugin.getConfig().getDouble("spawn-point.x", 0.0);
        double y = plugin.getConfig().getDouble("spawn-point.y", 64.0);
        double z = plugin.getConfig().getDouble("spawn-point.z", 0.0);
        float yaw = (float) plugin.getConfig().getDouble("spawn-point.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("spawn-point.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
