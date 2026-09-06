package com.crak.anticheat;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiXRay implements Listener {
    private final Main plugin;
    private final Set<Material> ores = new HashSet<>();
    private final Map<UUID, Integer> oreCount = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> oreTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Material, Integer>> oreTypes = new ConcurrentHashMap<>();

    public AntiXRay(Main plugin) {
        this.plugin = plugin;
        initializeOres();
    }

    private void initializeOres() {
        // 1.16 Compatible ores only
        ores.add(Material.DIAMOND_ORE);
        ores.add(Material.EMERALD_ORE);
        ores.add(Material.GOLD_ORE);
        ores.add(Material.IRON_ORE);
        ores.add(Material.REDSTONE_ORE);
        ores.add(Material.LAPIS_ORE);
        ores.add(Material.NETHER_QUARTZ_ORE);
        ores.add(Material.NETHER_GOLD_ORE);
        ores.add(Material.ANCIENT_DEBRIS);
        ores.add(Material.COAL_ORE);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        Block block = event.getBlock();
        if (!ores.contains(block.getType())) return;
        
        UUID uuid = player.getUniqueId();
        
        // Track ore types
        Map<Material, Integer> types = oreTypes.getOrDefault(uuid, new HashMap<>());
        types.put(block.getType(), types.getOrDefault(block.getType(), 0) + 1);
        oreTypes.put(uuid, types);
        
        // Check ore frequency
        int count = oreCount.getOrDefault(uuid, 0) + 1;
        oreCount.put(uuid, count);
        
        List<Long> timestamps = oreTimestamps.getOrDefault(uuid, new ArrayList<>());
        timestamps.add(System.currentTimeMillis());
        
        // Remove old timestamps (last 60 seconds)
        long currentTime = System.currentTimeMillis();
        timestamps.removeIf(t -> currentTime - t > 60000);
        oreTimestamps.put(uuid, timestamps);
        
        // Check if player mines too many ores in short time
        int maxOres = plugin.getConfig().getInt("xray.max-ores-per-minute", 15);
        if (timestamps.size() > maxOres) {
            if (isXRayPattern(player, timestamps)) {
                plugin.getPunishmentManager().addViolation(player, "X-Ray", "OreMining",
                    "Mined " + timestamps.size() + " ores in 60 seconds",
                    plugin.getConfig().getInt("violations.xray", 30));
                oreCount.put(uuid, 0);
                oreTimestamps.put(uuid, new ArrayList<>());
            }
        }
        
        checkValuableOres(player, block.getType());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        Block block = event.getBlockPlaced();
        if (ores.contains(block.getType())) {
            plugin.getPunishmentManager().addViolation(player, "X-Ray", "OrePlacement",
                "Placed " + block.getType().name(),
                plugin.getConfig().getInt("violations.xray", 30));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        oreCount.remove(uuid);
        oreTimestamps.remove(uuid);
        oreTypes.remove(uuid);
    }

    private boolean isXRayPattern(Player player, List<Long> timestamps) {
        if (timestamps.size() < 5) return false;
        
        long totalTime = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
        if (totalTime < 30000) {
            return true;
        }
        
        Map<Material, Integer> types = oreTypes.getOrDefault(player.getUniqueId(), new HashMap<>());
        int valuableCount = 0;
        int totalCount = 0;
        
        for (Map.Entry<Material, Integer> entry : types.entrySet()) {
            totalCount += entry.getValue();
            if (isValuableOre(entry.getKey())) {
                valuableCount += entry.getValue();
            }
        }
        
        return totalCount > 0 && (double) valuableCount / totalCount > 0.5;
    }

    private boolean isValuableOre(Material material) {
        return material == Material.DIAMOND_ORE || 
               material == Material.EMERALD_ORE ||
               material == Material.ANCIENT_DEBRIS ||
               material == Material.NETHER_GOLD_ORE;
    }

    private void checkValuableOres(Player player, Material material) {
        if (isValuableOre(material)) {
            UUID uuid = player.getUniqueId();
            Map<Material, Integer> types = oreTypes.getOrDefault(uuid, new HashMap<>());
            
            int valuableCount = 0;
            for (Map.Entry<Material, Integer> entry : types.entrySet()) {
                if (isValuableOre(entry.getKey())) {
                    valuableCount += entry.getValue();
                }
            }
            
            int maxValuable = plugin.getConfig().getInt("xray.max-valuable-ores", 10);
            if (valuableCount > maxValuable) {
                plugin.getPunishmentManager().addViolation(player, "X-Ray", "ValuableOres",
                    "Mined " + valuableCount + " valuable ores",
                    plugin.getConfig().getInt("violations.xray", 30));
            }
        }
    }

    public void resetPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        oreCount.remove(uuid);
        oreTimestamps.remove(uuid);
        oreTypes.remove(uuid);
    }
    
    // These methods are called by CheckManager
    public void checkBreak(BlockBreakEvent event) {
        onBlockBreak(event);
    }
    
    public void checkPlace(BlockPlaceEvent event) {
        onBlockPlace(event);
    }
}