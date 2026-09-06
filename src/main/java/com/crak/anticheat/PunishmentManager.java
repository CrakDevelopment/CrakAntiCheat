package com.crak.anticheat;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {
    private final Main plugin;
    private final Map<UUID, Map<String, Integer>> playerViolations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> violationTimestamps = new ConcurrentHashMap<>();
    
    // Softban storage
    private final Map<UUID, Long> softbannedPlayers = new ConcurrentHashMap<>();

    public PunishmentManager(Main plugin) {
        this.plugin = plugin;
    }

    public void addViolation(Player player, String category, String check, String details, int maxViolations) {
        UUID uuid = player.getUniqueId();
        
        Map<String, Integer> violations = playerViolations.getOrDefault(uuid, new HashMap<>());
        Map<String, Long> timestamps = violationTimestamps.getOrDefault(uuid, new HashMap<>());
        
        int count = violations.getOrDefault(check, 0) + 1;
        violations.put(check, count);
        timestamps.put(check, System.currentTimeMillis());
        
        playerViolations.put(uuid, violations);
        violationTimestamps.put(uuid, timestamps);
        
        // Send alert to staff
        plugin.getAlertManager().sendAlert(player, category, check, details, count);
        
        // Check if should ban
        if (count >= maxViolations) {
            String reason = category + " - " + check + " (VL: " + count + ")";
            banPlayer(player, reason);
            violations.put(check, 0);
        }
    }

    public void banPlayer(Player player, String reason) {
        FileConfiguration config = plugin.getConfig();
        String banType = config.getString("punishments." + getBanType(reason), "TEMP");
        
        if (banType.equalsIgnoreCase("PERM")) {
            permanentBan(player, reason);
        } else if (banType.equalsIgnoreCase("SOFTBAN")) {
            softBan(player, reason);
        } else {
            tempBan(player, reason, banType);
        }
    }

    private void permanentBan(Player player, String reason) {
        String name = player.getName();
        Bukkit.getBanList(BanList.Type.NAME).addBan(name, "§c[AntiCheat] §7" + reason, null, "Crak Anti-Cheat");
        player.kickPlayer("§cYou have been permanently banned!\n§7Reason: " + reason + "\n§7By: Crak Anti-Cheat");
        plugin.getLogger().info("§c" + name + " has been permanently banned for: " + reason);
    }

    private void tempBan(Player player, String reason, String banType) {
        String name = player.getName();
        long duration = getDurationFromString(banType);
        Date expiryDate = new Date(System.currentTimeMillis() + duration);
        
        Bukkit.getBanList(BanList.Type.NAME).addBan(name, "§c[AntiCheat] §7" + reason, expiryDate, "Crak Anti-Cheat");
        player.kickPlayer("§cYou have been temporarily banned!\n§7Reason: " + reason + 
                         "\n§7Duration: " + getDurationString(duration) + 
                         "\n§7By: Crak Anti-Cheat");
        plugin.getLogger().info("§c" + name + " has been temp banned for: " + reason + " (" + getDurationString(duration) + ")");
    }

    private void softBan(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        long duration = getDurationFromString("SOFTBAN");
        softbannedPlayers.put(uuid, System.currentTimeMillis() + duration);
        
        player.kickPlayer("§cYou have been softbanned!\n§7Reason: " + reason + 
                         "\n§7Duration: " + getDurationString(duration) + 
                         "\n§7By: Crak Anti-Cheat");
        plugin.getLogger().info("§c" + player.getName() + " has been softbanned for: " + reason);
    }

    private String getBanType(String reason) {
        if (reason.contains("KillAura") || reason.contains("Fly")) {
            return "PERM";
        } else if (reason.contains("X-Ray") || reason.contains("XRay")) {
            return "TEMP";
        } else {
            return "SOFTBAN";
        }
    }

    private long getDurationFromString(String duration) {
        FileConfiguration config = plugin.getConfig();
        String configPath = "durations." + duration.toLowerCase();
        
        if (config.contains(configPath)) {
            return config.getLong(configPath) * 1000; // Convert to milliseconds
        }
        
        // Default durations
        switch (duration.toUpperCase()) {
            case "PERM": return Long.MAX_VALUE;
            case "SOFTBAN": return 3600000; // 1 hour
            case "TEMP": return 86400000; // 24 hours
            default: return 3600000; // 1 hour
        }
    }

    private String getDurationString(long milliseconds) {
        if (milliseconds == Long.MAX_VALUE) return "Permanent";
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long years = days / 365;
        
        if (years > 0) return years + " year" + (years > 1 ? "s" : "");
        if (days > 0) return days + " day" + (days > 1 ? "s" : "");
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "");
        if (minutes > 0) return minutes + " minute" + (minutes > 1 ? "s" : "");
        return seconds + " second" + (seconds > 1 ? "s" : "");
    }

    public int getViolations(Player player, String check) {
        return playerViolations.getOrDefault(player.getUniqueId(), new HashMap<>())
            .getOrDefault(check, 0);
    }

    public void removePlayer(Player player) {
        playerViolations.remove(player.getUniqueId());
        violationTimestamps.remove(player.getUniqueId());
        softbannedPlayers.remove(player.getUniqueId());
    }

    public boolean isSoftbanned(Player player) {
        UUID uuid = player.getUniqueId();
        if (!softbannedPlayers.containsKey(uuid)) return false;
        
        long expiryTime = softbannedPlayers.get(uuid);
        if (System.currentTimeMillis() > expiryTime) {
            softbannedPlayers.remove(uuid);
            return false;
        }
        return true;
    }
}