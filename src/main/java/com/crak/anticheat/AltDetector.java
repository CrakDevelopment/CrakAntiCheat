package com.crak.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AltDetector implements Listener {
    private final Main plugin;
    private final Map<String, List<UUID>> ipHistory = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerIPs = new ConcurrentHashMap<>();
    private final Map<String, Integer> accountCount = new HashMap<>();

    public AltDetector(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = getPlayerIP(player);
        
        if (ip == null) return;
        
        playerIPs.put(player.getUniqueId(), ip);
        
        // Check for alt accounts
        checkForAlts(player, ip);
        
        // Check if player is softbanned
        if (plugin.getPunishmentManager().isSoftbanned(player)) {
            player.kickPlayer("§cYou are softbanned!\n§7Please wait for your ban to expire.");
            return;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up
        playerIPs.remove(event.getPlayer().getUniqueId());
    }

    private void checkForAlts(Player player, String ip) {
        List<UUID> accounts = ipHistory.getOrDefault(ip, new ArrayList<>());
        
        // Check for existing accounts on this IP
        if (!accounts.isEmpty()) {
            // Count accounts
            int count = accounts.size() + 1;
            
            // Check if this is an alt
            if (count >= 2) {
                String message = "§c[Alt Detection] §7" + player.getName() + " is using IP " + ip + 
                               " which has " + count + " accounts!";
                
                // Alert staff
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (online.hasPermission("crakanticheat.staff")) {
                        online.sendMessage(message);
                    }
                }
                
                // Log it
                plugin.getLogger().info("Alt detected: " + player.getName() + " (IP: " + ip + ", Accounts: " + count + ")");
                
                // Check config for alt punishment
                if (plugin.getConfig().getBoolean("alt-detection.punish", true)) {
                    int maxAccounts = plugin.getConfig().getInt("alt-detection.max-accounts", 3);
                    if (count > maxAccounts) {
                        String punishment = plugin.getConfig().getString("alt-detection.punishment", "SOFTBAN");
                        plugin.getPunishmentManager().banPlayer(player, "Alt Account (IP: " + ip + ")");
                    }
                }
            }
        }
        
        // Add to history
        accounts.add(player.getUniqueId());
        ipHistory.put(ip, accounts);
        accountCount.put(ip, accounts.size());
    }

    private String getPlayerIP(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null) return null;
        
        String ip = address.getAddress().getHostAddress();
        // Handle IPv6
        if (ip.contains(":")) {
            ip = ip.substring(0, ip.indexOf(":"));
        }
        return ip;
    }

    public int getAccountCount(String ip) {
        return accountCount.getOrDefault(ip, 0);
    }

    public List<UUID> getAccountsOnIP(String ip) {
        return ipHistory.getOrDefault(ip, new ArrayList<>());
    }

    public void clearIPHistory() {
        ipHistory.clear();
        accountCount.clear();
        playerIPs.clear();
    }
}