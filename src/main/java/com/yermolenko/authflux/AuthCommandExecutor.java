package com.yermolenko.authflux;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;


public class AuthCommandExecutor implements CommandExecutor {
    private final AuthFlux plugin;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 24;

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

        if (command.getName().equalsIgnoreCase("reg")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("reg-usage"))));
                return true;
            }

            String password = args[0];
            if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
                String message = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("password-length-invalid")))
                        .replace("%min%", String.valueOf(MIN_PASSWORD_LENGTH))
                        .replace("%max%", String.valueOf(MAX_PASSWORD_LENGTH));
                player.sendMessage(message);
                return true;
            }

            if (dbManager.isPlayerRegistered(uuid)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("reg-already-registered"))));
                return true;
            }

            dbManager.registerPlayer(uuid, player.getName(), password);
            dbManager.setPlayerLoggedIn(uuid, true);
            unfreezeAndTeleport(player, uuid);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("reg-success"))));
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
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        plugin.getLogger().info("Unfroze player " + player.getName());

        // Teleport to saved location
        Location savedLocation = plugin.getDatabaseManager().getPlayerLocation(uuid);
        if (savedLocation != null && savedLocation.getWorld() != null) {
            player.teleport(savedLocation);
            plugin.getLogger().info("Teleported " + player.getName() + " to saved location: " + savedLocation);
        } else {
            plugin.getLogger().warning("No valid saved location for " + player.getName());
        }
    }
}
