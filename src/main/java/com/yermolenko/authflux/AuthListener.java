package com.yermolenko.authflux;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class AuthListener implements Listener {
    private final AuthFlux plugin;
    private final Location spawnPoint;

    public AuthListener(AuthFlux plugin) {
        this.plugin = plugin;
        this.spawnPoint = loadSpawnPoint();
    }

    private Location loadSpawnPoint() {
        World world = plugin.getServer().getWorld(plugin.getConfig().getString("spawn-point.world", "world"));
        if (world == null) {
            plugin.getLogger().warning("Spawn world not found! Using default world.");
            world = plugin.getServer().getWorlds().getFirst();
        }
        double x = plugin.getConfig().getDouble("spawn-point.x", 0.0);
        double y = plugin.getConfig().getDouble("spawn-point.y", 64.0);
        double z = plugin.getConfig().getDouble("spawn-point.z", 0.0);
        float yaw = (float) plugin.getConfig().getDouble("spawn-point.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("spawn-point.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DatabaseManager dbManager = plugin.getDatabaseManager();

        // Save initial location and teleport to spawn
        dbManager.savePlayerLocation(player.getUniqueId().toString(), player.getLocation());
        player.teleport(spawnPoint);

        // Freeze player
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 128, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 128, false, false));

        if (!dbManager.isPlayerRegistered(player.getUniqueId().toString())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("join-new-player"))));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("join-returning-player"))));
        }
        dbManager.setPlayerLoggedIn(player.getUniqueId().toString(), false);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DatabaseManager dbManager = plugin.getDatabaseManager();
        String uuid = player.getUniqueId().toString();
        boolean isLoggedIn = dbManager.isPlayerLoggedIn(uuid);

        if (!isLoggedIn) {
            event.setCancelled(true);
            plugin.getLogger().info("Movement cancelled for " + player.getName() + " (not logged in)");
            if (!dbManager.isPlayerRegistered(uuid)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("move-not-registered"))));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getMessagesConfig().getString("move-not-logged-in"))));
            }
        } else {
            plugin.getLogger().info("Movement allowed for " + player.getName() + " (logged in)");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DatabaseManager dbManager = plugin.getDatabaseManager();
        String uuid = player.getUniqueId().toString();
        if (dbManager.isPlayerLoggedIn(uuid)) {
            dbManager.savePlayerLocation(uuid, player.getLocation());
            dbManager.setPlayerLoggedIn(uuid, false);
        }
    }
}
