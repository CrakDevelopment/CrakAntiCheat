package com.crak.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.BanList;

import java.util.UUID;

public class AntiCheatCommand implements CommandExecutor {
    private final Main plugin;

    public AntiCheatCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "alerts":
                plugin.getAlertManager().toggleAlerts(player);
                break;

            case "reload":
                if (!player.hasPermission("crakanticheat.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission!");
                    return true;
                }
                plugin.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                break;

            case "reset":
                if (!player.hasPermission("crakanticheat.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /cac reset <player>");
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                plugin.getPunishmentManager().removePlayer(target);
                plugin.getAntiXRay().resetPlayerData(target);
                player.sendMessage(ChatColor.GREEN + "Reset data for " + target.getName());
                break;

            case "info":
                player.sendMessage(ChatColor.GOLD + "=== Crak Anti-Cheat Info ===");
                player.sendMessage(ChatColor.GRAY + "Version: " + plugin.getDescription().getVersion());
                player.sendMessage(ChatColor.GRAY + "Server: " + plugin.getServer().getVersion());
                player.sendMessage(ChatColor.GRAY + "Paper: " + plugin.isPaper());
                player.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Active");
                break;

            case "check":
                if (!player.hasPermission("crakanticheat.staff")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /cac check <player>");
                    return true;
                }
                Player checkTarget = plugin.getServer().getPlayer(args[1]);
                if (checkTarget == null) {
                    player.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                showPlayerInfo(player, checkTarget);
                break;

            case "ban":
                if (!player.hasPermission("crakanticheat.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission!");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /cac ban <player> <reason>");
                    return true;
                }
                Player banTarget = plugin.getServer().getPlayer(args[1]);
                if (banTarget == null) {
                    player.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                StringBuilder reason = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    reason.append(args[i]).append(" ");
                }
                plugin.getPunishmentManager().banPlayer(banTarget, "Manual: " + reason.toString().trim());
                player.sendMessage(ChatColor.GREEN + "Banned " + banTarget.getName());
                break;

            case "unban":
                if (!player.hasPermission("crakanticheat.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /cac unban <player>");
                    return true;
                }
                plugin.getServer().getBanList(BanList.Type.NAME).pardon(args[1]);
                player.sendMessage(ChatColor.GREEN + "Unbanned " + args[1]);
                break;

            case "stats":
                if (!player.hasPermission("crakanticheat.staff")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission!");
                    return true;
                }
                showStats(player);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Crak Anti-Cheat Commands ===");
        player.sendMessage(ChatColor.GRAY + "/cac alerts " + ChatColor.WHITE + "- Toggle alerts");
        player.sendMessage(ChatColor.GRAY + "/cac check <player> " + ChatColor.WHITE + "- Check violations");
        player.sendMessage(ChatColor.GRAY + "/cac info " + ChatColor.WHITE + "- Plugin info");
        player.sendMessage(ChatColor.GRAY + "/cac stats " + ChatColor.WHITE + "- Server stats");
        
        if (player.hasPermission("crakanticheat.admin")) {
            player.sendMessage(ChatColor.GRAY + "/cac reload " + ChatColor.WHITE + "- Reload config");
            player.sendMessage(ChatColor.GRAY + "/cac reset <player> " + ChatColor.WHITE + "- Reset player");
            player.sendMessage(ChatColor.GRAY + "/cac ban <player> <reason> " + ChatColor.WHITE + "- Ban");
            player.sendMessage(ChatColor.GRAY + "/cac unban <player> " + ChatColor.WHITE + "- Unban");
        }
    }

    private void showPlayerInfo(Player staff, Player target) {
        staff.sendMessage(ChatColor.GOLD + "=== " + target.getName() + " - Anti-Cheat Info ===");
        
        // Check all violations
        String[][] checks = {
            {"Speed", "speedVL"},
            {"Fly", "flyVL"},
            {"NoFall", "nofallVL"},
            {"Timer", "timerVL"},
            {"Phase/Noclip", "phaseVL"},
            {"Scaffold", "scaffoldVL"},
            {"Jesus", "jesusVL"},
            {"Spider", "spiderVL"},
            {"Step", "stepVL"},
            {"ESP", "espVL"},
            {"Velocity", "velocityVL"},
            {"KillAura", "killauraVL"},
            {"Reach", "reachVL"},
            {"Aimbot", "aimbotVL"},
            {"TriggerBot", "triggerbotVL"},
            {"HitBox", "hitboxVL"},
            {"Criticals", "criticalsVL"},
            {"BowAura", "bowauraVL"},
            {"FastClick", "fastclickVL"},
            {"FastBreak", "fastbreakVL"},
            {"FastPlace", "fastplaceVL"},
            {"FastConsume", "fastconsumeVL"},
            {"FastDrop", "fastdropVL"},
            {"AutoEat", "autoeatVL"},
            {"Dupe", "dupeVL"},
            {"GodMode", "godmodeVL"},
            {"ChatSpam", "spamVL"},
            {"Advertising", "advertiseVL"},
            {"Swearing", "swearingVL"}
        };
        
        // This is simplified - in real implementation, you'd get from CheckManager
        for (String[] check : checks) {
            int vl = 0; // Get from maps
            staff.sendMessage(ChatColor.GRAY + check[0] + ": " + getColorForVL(vl) + vl);
        }
        
        if (plugin.getPunishmentManager().isSoftbanned(target)) {
            staff.sendMessage(ChatColor.RED + "Status: " + ChatColor.RED + "SOFTBANNED");
        } else {
            staff.sendMessage(ChatColor.GREEN + "Status: " + ChatColor.GREEN + "CLEAN");
        }
    }

    private void showStats(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Server Statistics ===");
        player.sendMessage(ChatColor.GRAY + "Online Players: " + plugin.getServer().getOnlinePlayers().size());
        player.sendMessage(ChatColor.GRAY + "Total Bans: " + plugin.getServer().getBanList(BanList.Type.NAME).getBanEntries().size());
        player.sendMessage(ChatColor.GRAY + "Server Type: " + plugin.getServer().getName());
        player.sendMessage(ChatColor.GRAY + "Version: " + plugin.getServer().getVersion());
        player.sendMessage(ChatColor.GRAY + "Paper: " + plugin.isPaper());
    }

    private String getColorForVL(int violations) {
        if (violations >= 15) return ChatColor.RED + String.valueOf(violations);
        if (violations >= 10) return ChatColor.YELLOW + String.valueOf(violations);
        if (violations >= 5) return ChatColor.GOLD + String.valueOf(violations);
        return ChatColor.GREEN + String.valueOf(violations);
    }
}