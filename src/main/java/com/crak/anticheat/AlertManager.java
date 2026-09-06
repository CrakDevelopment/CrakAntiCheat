package com.crak.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AlertManager {
    private final Main plugin;
    private final Set<UUID> staffAlertsEnabled = new HashSet<>();

    public AlertManager(Main plugin) {
        this.plugin = plugin;
    }

    public void sendAlert(Player player, String category, String check, String details, int violations) {
        FileConfiguration config = plugin.getConfig();
        
        // Check if alerts are enabled
        if (!config.getBoolean("alerts.enabled", true)) return;
        
        // Check if we should alert for this type
        String alertType = config.getString("alerts." + category.toLowerCase() + ".enabled", "true");
        if (!Boolean.parseBoolean(alertType)) return;
        
        // Get alert format
        String prefix = ChatColor.translateAlternateColorCodes('&', 
            config.getString("alerts.prefix", "&8[&cAntiCheat&8]&r"));
        
        String message = ChatColor.translateAlternateColorCodes('&',
            config.getString("alerts.format", 
                "&c{player} &7failed &c{check} &7(VL: {violations}) &7- &f{details}"));
        
        // Replace placeholders
        message = message.replace("{player}", player.getName())
                        .replace("{category}", category)
                        .replace("{check}", check)
                        .replace("{details}", details)
                        .replace("{violations}", String.valueOf(violations));
        
        // Send to all staff
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("crakanticheat.staff")) {
                if (staffAlertsEnabled.contains(staff.getUniqueId())) {
                    staff.sendMessage(prefix + " " + message);
                }
            }
        }
        
        // Log to console if enabled
        if (config.getBoolean("alerts.log-to-console", true)) {
            plugin.getLogger().info("[AntiCheat] " + player.getName() + " | " + category + 
                                  " | " + check + " | VL: " + violations);
        }
    }

    public void sendStaffMessage(String message) {
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("crakanticheat.staff") && staffAlertsEnabled.contains(staff.getUniqueId())) {
                staff.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }

    public void toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (staffAlertsEnabled.contains(uuid)) {
            staffAlertsEnabled.remove(uuid);
            player.sendMessage(ChatColor.RED + "Anti-Cheat alerts disabled!");
        } else {
            staffAlertsEnabled.add(uuid);
            player.sendMessage(ChatColor.GREEN + "Anti-Cheat alerts enabled!");
        }
    }

    public boolean hasAlertsEnabled(Player player) {
        return staffAlertsEnabled.contains(player.getUniqueId());
    }

    public void enableAlerts(Player player) {
        staffAlertsEnabled.add(player.getUniqueId());
    }

    public void disableAlerts(Player player) {
        staffAlertsEnabled.remove(player.getUniqueId());
    }
}