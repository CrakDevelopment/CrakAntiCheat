package com.crak.anticheat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class CheckManager implements Listener {
    private final Main plugin;
    
    // All check maps
    private final Map<UUID, Double> lastY = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();  // ADDED THIS
    private final Map<UUID, Integer> airTicks = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, Long> lastBowShot = new HashMap<>();
    private final Map<UUID, Long> lastInteract = new HashMap<>();
    private final Map<UUID, Long> lastBlockBreak = new HashMap<>();
    private final Map<UUID, Long> lastBlockPlace = new HashMap<>();
    private final Map<UUID, Long> lastItemConsume = new HashMap<>();
    private final Map<UUID, Long> lastItemDrop = new HashMap<>();
    private final Map<UUID, Long> lastChat = new HashMap<>();
    private final Map<UUID, Long> lastCommand = new HashMap<>();
    private final Map<UUID, List<Location>> blockPlaceHistory = new HashMap<>();
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Double> lastHealth = new HashMap<>();
    private final Map<UUID, Double> lastVelocityX = new HashMap<>();
    private final Map<UUID, Double> lastVelocityZ = new HashMap<>();
    
    // Violation counters
    private final Map<UUID, Integer> speedVL = new HashMap<>();
    private final Map<UUID, Integer> flyVL = new HashMap<>();
    private final Map<UUID, Integer> nofallVL = new HashMap<>();
    private final Map<UUID, Integer> timerVL = new HashMap<>();
    private final Map<UUID, Integer> velocityVL = new HashMap<>();
    private final Map<UUID, Integer> criticalsVL = new HashMap<>();
    private final Map<UUID, Integer> aimbotVL = new HashMap<>();
    private final Map<UUID, Integer> triggerbotVL = new HashMap<>();
    private final Map<UUID, Integer> hitboxVL = new HashMap<>();
    private final Map<UUID, Integer> reachVL = new HashMap<>();
    private final Map<UUID, Integer> killauraVL = new HashMap<>();
    private final Map<UUID, Integer> bowauraVL = new HashMap<>();
    private final Map<UUID, Integer> fastclickVL = new HashMap<>();
    private final Map<UUID, Integer> fastbreakVL = new HashMap<>();
    private final Map<UUID, Integer> fastplaceVL = new HashMap<>();
    private final Map<UUID, Integer> fastconsumeVL = new HashMap<>();
    private final Map<UUID, Integer> fastdropVL = new HashMap<>();
    private final Map<UUID, Integer> phaseVL = new HashMap<>();
    private final Map<UUID, Integer> scaffoldVL = new HashMap<>();
    private final Map<UUID, Integer> jesusVL = new HashMap<>();
    private final Map<UUID, Integer> spiderVL = new HashMap<>();
    private final Map<UUID, Integer> stepVL = new HashMap<>();
    private final Map<UUID, Integer> godmodeVL = new HashMap<>();
    private final Map<UUID, Integer> dupeVL = new HashMap<>();
    private final Map<UUID, Integer> espVL = new HashMap<>();
    private final Map<UUID, Integer> autoeatVL = new HashMap<>();
    private final Map<UUID, Integer> spamVL = new HashMap<>();
    private final Map<UUID, Integer> advertiseVL = new HashMap<>();
    private final Map<UUID, Integer> swearingVL = new HashMap<>();

    // Patterns for chat detection
    private final Pattern ipPattern = Pattern.compile(
        "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b|\\b(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\\b"
    );
    private final Pattern swearPattern = Pattern.compile(
        "(?i)\\b(fuck|shit|ass|bitch|cunt|damn|hell|dick|pussy|whore|slut)\\b"
    );

    public CheckManager(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        
        UUID uuid = player.getUniqueId();
        double deltaX = to.getX() - from.getX();
        double deltaY = to.getY() - from.getY();
        double deltaZ = to.getZ() - from.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        
        // Movement Checks
        checkSpeed(player, distance);
        checkFly(player, deltaY);
        checkNoFall(player, from, to);
        checkTimer(player);
        checkPhase(player);
        checkScaffold(player);
        checkJesus(player, to);
        checkSpider(player, deltaY);
        checkStep(player, from, to);
        checkESP(player);
        checkVelocity(player, from, to);
        
        // Track air ticks
        if (!player.isOnGround()) {
            airTicks.put(uuid, airTicks.getOrDefault(uuid, 0) + 1);
        } else {
            airTicks.put(uuid, 0);
        }
        
        lastY.put(uuid, to.getY());
        lastMoveTime.put(uuid, System.currentTimeMillis());
        lastLocation.put(uuid, to.clone());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        Player target = (Player) event.getEntity();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastHit = lastHitTime.getOrDefault(uuid, 0L);
        long timeSinceLastHit = currentTime - lastHit;
        
        checkKillAura(player, timeSinceLastHit);
        checkReach(player, target);
        checkAimbot(player, target);
        checkTriggerBot(player, target);
        checkHitBox(player, target);
        checkCriticals(player, event);
        
        lastHitTime.put(uuid, currentTime);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        checkBowAura(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        checkFastClick(player, currentTime);
        
        if (event.getItem() != null && event.getItem().getType().isEdible()) {
            checkAutoEat(player);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        checkFastConsume(player);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        checkFastDrop(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        checkFastBreak(player);
        plugin.getAntiXRay().checkBreak(event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        checkFastPlace(player);
        checkScaffoldPlace(player, event.getBlock().getLocation());
        plugin.getAntiXRay().checkPlace(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        checkDupe(player, event);
        checkGodMode(player);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        int foodLevel = event.getFoodLevel();
        int previousFood = player.getFoodLevel();
        
        if (foodLevel > previousFood + 10 && !player.hasPotionEffect(PotionEffectType.SATURATION)) {
            godmodeVL.put(uuid, godmodeVL.getOrDefault(uuid, 0) + 1);
            if (godmodeVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "Combat", "GodMode",
                    "Suspicious food change: " + (foodLevel - previousFood),
                    plugin.getConfig().getInt("violations.godmode", 10));
                godmodeVL.put(uuid, 0);
            }
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        String message = event.getMessage();
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Chat Spam Check
        Long lastChatTime = lastChat.get(uuid);
        if (lastChatTime != null && currentTime - lastChatTime < 1000) {
            spamVL.put(uuid, spamVL.getOrDefault(uuid, 0) + 1);
            if (spamVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "Chat", "Spam",
                    "Chat spam detected",
                    plugin.getConfig().getInt("violations.spam", 10));
                spamVL.put(uuid, 0);
            }
        } else {
            spamVL.put(uuid, Math.max(0, spamVL.getOrDefault(uuid, 0) - 1));
        }
        
        // Advertising Check
        if (ipPattern.matcher(message).find()) {
            advertiseVL.put(uuid, advertiseVL.getOrDefault(uuid, 0) + 1);
            if (advertiseVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Chat", "Advertising",
                    "IP/Server advertising detected",
                    plugin.getConfig().getInt("violations.advertise", 5));
                advertiseVL.put(uuid, 0);
            }
        }
        
        // Swearing Check
        if (swearPattern.matcher(message).find()) {
            swearingVL.put(uuid, swearingVL.getOrDefault(uuid, 0) + 1);
            if (swearingVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "Chat", "Swearing",
                    "Profanity detected",
                    plugin.getConfig().getInt("violations.swearing", 10));
                swearingVL.put(uuid, 0);
            }
        }
        
        lastChat.put(uuid, currentTime);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crakanticheat.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastCommandTime = lastCommand.get(uuid);
        
        if (lastCommandTime != null && currentTime - lastCommandTime < 500) {
            if (plugin.getConfig().getBoolean("anti-commandspam.enabled", true)) {
                event.setCancelled(true);
                player.sendMessage("§cPlease wait before using commands again!");
            }
        }
        
        lastCommand.put(uuid, currentTime);
    }

    // ============================================
    // CHECK METHODS
    // ============================================

    private void checkSpeed(Player player, double distance) {
        if (player.isFlying() || player.isInsideVehicle()) return;
        
        UUID uuid = player.getUniqueId();
        double maxSpeed = 0.3;
        
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            maxSpeed += (amplifier + 1) * 0.15;
        }
        
        if (distance > maxSpeed) {
            speedVL.put(uuid, speedVL.getOrDefault(uuid, 0) + 1);
            if (speedVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "Movement", "Speed",
                    "Distance: " + String.format("%.3f", distance),
                    plugin.getConfig().getInt("violations.speed", 10));
                speedVL.put(uuid, 0);
            }
        } else {
            speedVL.put(uuid, Math.max(0, speedVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkFly(Player player, double deltaY) {
        if (player.isFlying() || player.isInsideVehicle()) return;
        if (player.getAllowFlight()) return;
        
        UUID uuid = player.getUniqueId();
        if (deltaY > 1.5 && !player.isOnGround()) {
            flyVL.put(uuid, flyVL.getOrDefault(uuid, 0) + 1);
            if (flyVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Movement", "Fly",
                    "DeltaY: " + String.format("%.3f", deltaY),
                    plugin.getConfig().getInt("violations.fly", 15));
                flyVL.put(uuid, 0);
            }
        } else {
            flyVL.put(uuid, Math.max(0, flyVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkNoFall(Player player, Location from, Location to) {
        if (player.isFlying() || player.isInsideVehicle()) return;
        
        UUID uuid = player.getUniqueId();
        double fallDistance = from.getY() - to.getY();
        
        if (player.isOnGround() && fallDistance > 3.0) {
            nofallVL.put(uuid, nofallVL.getOrDefault(uuid, 0) + 1);
            if (nofallVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Movement", "NoFall",
                    "Fell " + String.format("%.3f", fallDistance) + " blocks",
                    plugin.getConfig().getInt("violations.nofall", 20));
                nofallVL.put(uuid, 0);
            }
        } else {
            nofallVL.put(uuid, Math.max(0, nofallVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkTimer(Player player) {
        if (player.isFlying() || player.isInsideVehicle()) return;
        
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastMove = lastMoveTime.get(uuid);
        
        if (lastMove != null) {
            long timeDiff = currentTime - lastMove;
            if (timeDiff < 45) {
                timerVL.put(uuid, timerVL.getOrDefault(uuid, 0) + 1);
                if (timerVL.getOrDefault(uuid, 0) >= 5) {
                    plugin.getPunishmentManager().addViolation(player, "Movement", "Timer",
                        "Tick speed: " + timeDiff + "ms",
                        plugin.getConfig().getInt("violations.timer", 10));
                    timerVL.put(uuid, 0);
                }
            } else {
                timerVL.put(uuid, Math.max(0, timerVL.getOrDefault(uuid, 0) - 1));
            }
        }
    }

    private void checkPhase(Player player) {
        UUID uuid = player.getUniqueId();
        Location eyeLoc = player.getEyeLocation();
        Material blockType = eyeLoc.getBlock().getType();
        
        if (blockType != Material.AIR && blockType != Material.CAVE_AIR && 
            blockType != Material.VOID_AIR) {
            phaseVL.put(uuid, phaseVL.getOrDefault(uuid, 0) + 1);
            if (phaseVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Movement", "Phase/Noclip",
                    "Inside block: " + blockType.name(),
                    plugin.getConfig().getInt("violations.phase", 10));
                phaseVL.put(uuid, 0);
            }
        } else {
            phaseVL.put(uuid, Math.max(0, phaseVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkScaffold(Player player) {
        if (player.isFlying() || player.isInsideVehicle()) return;
        if (player.getAllowFlight()) return;
        
        UUID uuid = player.getUniqueId();
        List<Location> history = blockPlaceHistory.getOrDefault(uuid, new ArrayList<>());
        
        if (player.isOnGround() && history.size() > 5) {
            Location below = player.getLocation().subtract(0, 1, 0);
            Location belowBlock = below.getBlock().getLocation();
            
            if (history.stream().anyMatch(loc -> loc.equals(belowBlock))) {
                scaffoldVL.put(uuid, scaffoldVL.getOrDefault(uuid, 0) + 1);
                if (scaffoldVL.getOrDefault(uuid, 0) >= 3) {
                    plugin.getPunishmentManager().addViolation(player, "Movement", "Scaffold",
                        "Suspicious block placement pattern",
                        plugin.getConfig().getInt("violations.scaffold", 10));
                    scaffoldVL.put(uuid, 0);
                }
            }
        }
        
        if (history.size() > 20) {
            history.remove(0);
            blockPlaceHistory.put(uuid, history);
        }
    }

    private void checkScaffoldPlace(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        List<Location> history = blockPlaceHistory.getOrDefault(uuid, new ArrayList<>());
        history.add(location.clone());
        blockPlaceHistory.put(uuid, history);
    }

    private void checkJesus(Player player, Location to) {
        if (player.isFlying() || player.isInsideVehicle()) return;
        if (player.getAllowFlight()) return;
        
        UUID uuid = player.getUniqueId();
        Material below = to.clone().subtract(0, 1, 0).getBlock().getType();
        
        if ((below == Material.WATER || below == Material.LAVA) && !player.isOnGround()) {
            jesusVL.put(uuid, jesusVL.getOrDefault(uuid, 0) + 1);
            if (jesusVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Movement", "Jesus",
                    "Walking on " + below.name(),
                    plugin.getConfig().getInt("violations.jesus", 10));
                jesusVL.put(uuid, 0);
            }
        } else {
            jesusVL.put(uuid, Math.max(0, jesusVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkSpider(Player player, double deltaY) {
        if (player.isFlying() || player.isInsideVehicle()) return;
        if (player.getAllowFlight()) return;
        if (player.isOnGround()) return;
        
        UUID uuid = player.getUniqueId();
        if (deltaY > 0.1 && player.getLocation().getBlock().getType().isSolid()) {
            spiderVL.put(uuid, spiderVL.getOrDefault(uuid, 0) + 1);
            if (spiderVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Movement", "Spider",
                    "Climbing wall",
                    plugin.getConfig().getInt("violations.spider", 10));
                spiderVL.put(uuid, 0);
            }
        } else {
            spiderVL.put(uuid, Math.max(0, spiderVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkStep(Player player, Location from, Location to) {
        if (player.isFlying() || player.isInsideVehicle()) return;
        if (player.getAllowFlight()) return;
        
        UUID uuid = player.getUniqueId();
        double stepHeight = to.getY() - from.getY();
        
        if (stepHeight > 0.6 && stepHeight < 1.0 && player.isOnGround()) {
            stepVL.put(uuid, stepVL.getOrDefault(uuid, 0) + 1);
            if (stepVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Movement", "Step",
                    "Step height: " + String.format("%.2f", stepHeight),
                    plugin.getConfig().getInt("violations.step", 10));
                stepVL.put(uuid, 0);
            }
        } else {
            stepVL.put(uuid, Math.max(0, stepVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkESP(Player player) {
        UUID uuid = player.getUniqueId();
        List<Entity> nearby = player.getNearbyEntities(50, 50, 50);
        int visible = 0;
        int total = 0;
        
        for (Entity entity : nearby) {
            if (entity instanceof Player) {
                total++;
                if (player.hasLineOfSight(entity)) {
                    visible++;
                }
            }
        }
        
        if (total > 0 && visible > total * 0.8) {
            espVL.put(uuid, espVL.getOrDefault(uuid, 0) + 1);
            if (espVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "ESP", "PlayerESP",
                    "Looking at " + visible + "/" + total + " players",
                    plugin.getConfig().getInt("violations.esp", 10));
                espVL.put(uuid, 0);
            }
        } else {
            espVL.put(uuid, Math.max(0, espVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkVelocity(Player player, Location from, Location to) {
        UUID uuid = player.getUniqueId();
        double velocityX = to.getX() - from.getX();
        double velocityZ = to.getZ() - from.getZ();
        
        if (lastVelocityX.containsKey(uuid) && lastVelocityZ.containsKey(uuid)) {
            double prevX = lastVelocityX.get(uuid);
            double prevZ = lastVelocityZ.get(uuid);
            
            if (Math.abs(velocityX - prevX) < 0.1 && Math.abs(velocityZ - prevZ) < 0.1) {
                velocityVL.put(uuid, velocityVL.getOrDefault(uuid, 0) + 1);
                if (velocityVL.getOrDefault(uuid, 0) >= 3) {
                    plugin.getPunishmentManager().addViolation(player, "Combat", "Velocity",
                        "No knockback detected",
                        plugin.getConfig().getInt("violations.velocity", 10));
                    velocityVL.put(uuid, 0);
                }
            }
        }
        
        lastVelocityX.put(uuid, velocityX);
        lastVelocityZ.put(uuid, velocityZ);
    }

    private void checkKillAura(Player player, long timeSinceLastHit) {
        UUID uuid = player.getUniqueId();
        if (timeSinceLastHit < 200) {
            killauraVL.put(uuid, killauraVL.getOrDefault(uuid, 0) + 1);
            if (killauraVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Combat", "KillAura",
                    "Hit delay: " + timeSinceLastHit + "ms",
                    plugin.getConfig().getInt("violations.killaura", 20));
                killauraVL.put(uuid, 0);
            }
        } else {
            killauraVL.put(uuid, Math.max(0, killauraVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkReach(Player player, Player target) {
        UUID uuid = player.getUniqueId();
        double distance = player.getLocation().distance(target.getLocation());
        
        if (distance > 6.0) {
            reachVL.put(uuid, reachVL.getOrDefault(uuid, 0) + 1);
            if (reachVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Combat", "Reach",
                    "Distance: " + String.format("%.2f", distance) + " blocks",
                    plugin.getConfig().getInt("violations.reach", 10));
                reachVL.put(uuid, 0);
            }
        } else {
            reachVL.put(uuid, Math.max(0, reachVL.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkAimbot(Player player, Player target) {
        UUID uuid = player.getUniqueId();
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        Location eyeLoc = player.getEyeLocation();
        Location targetHead = target.getEyeLocation();
        double distance = eyeLoc.distance(targetHead);
        
        if (distance < 10) {
            double diffX = targetHead.getX() - eyeLoc.getX();
            double diffY = targetHead.getY() - eyeLoc.getY();
            double diffZ = targetHead.getZ() - eyeLoc.getZ();
            
            double requiredYaw = Math.toDegrees(Math.atan2(-diffX, diffZ));
            double requiredPitch = Math.toDegrees(Math.atan2(-diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)));
            
            double yawDiff = Math.abs(yaw - requiredYaw);
            double pitchDiff = Math.abs(pitch - requiredPitch);
            
            if (yawDiff < 0.5 && pitchDiff < 0.5) {
                aimbotVL.put(uuid, aimbotVL.getOrDefault(uuid, 0) + 1);
                if (aimbotVL.getOrDefault(uuid, 0) >= 3) {
                    plugin.getPunishmentManager().addViolation(player, "Combat", "Aimbot",
                        "Perfect aim detected",
                        plugin.getConfig().getInt("violations.aimbot", 10));
                    aimbotVL.put(uuid, 0);
                }
            }
        }
    }

    private void checkTriggerBot(Player player, Player target) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastHit = lastHitTime.getOrDefault(uuid, 0L);
        
        if (currentTime - lastHit < 50) {
            triggerbotVL.put(uuid, triggerbotVL.getOrDefault(uuid, 0) + 1);
            if (triggerbotVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Combat", "TriggerBot",
                    "Instant attack on crosshair",
                    plugin.getConfig().getInt("violations.triggerbot", 10));
                triggerbotVL.put(uuid, 0);
            }
        }
    }

    private void checkHitBox(Player player, Player target) {
        UUID uuid = player.getUniqueId();
        double hitboxSize = target.getBoundingBox().getWidthX() / 2.0;
        if (hitboxSize > 0.9) {
            hitboxVL.put(uuid, hitboxVL.getOrDefault(uuid, 0) + 1);
            if (hitboxVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Combat", "HitBox",
                    "Abnormal hitbox size",
                    plugin.getConfig().getInt("violations.hitbox", 10));
                hitboxVL.put(uuid, 0);
            }
        }
    }

    private void checkCriticals(Player player, EntityDamageByEntityEvent event) {
        UUID uuid = player.getUniqueId();
        if (event.getDamage() == event.getDamage() * 1.5) {
            if (player.isOnGround() || player.getFallDistance() < 0.1) {
                criticalsVL.put(uuid, criticalsVL.getOrDefault(uuid, 0) + 1);
                if (criticalsVL.getOrDefault(uuid, 0) >= 3) {
                    plugin.getPunishmentManager().addViolation(player, "Combat", "Criticals",
                        "Critical hit on ground",
                        plugin.getConfig().getInt("violations.criticals", 10));
                    criticalsVL.put(uuid, 0);
                }
            }
        }
    }

    private void checkBowAura(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastShot = lastBowShot.getOrDefault(uuid, 0L);
        long timeSinceLastShot = currentTime - lastShot;
        
        if (timeSinceLastShot < 500) {
            bowauraVL.put(uuid, bowauraVL.getOrDefault(uuid, 0) + 1);
            if (bowauraVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Combat", "BowAura",
                    "Bow shot delay: " + timeSinceLastShot + "ms",
                    plugin.getConfig().getInt("violations.bowaura", 15));
                bowauraVL.put(uuid, 0);
            }
        } else {
            bowauraVL.put(uuid, Math.max(0, bowauraVL.getOrDefault(uuid, 0) - 1));
        }
        lastBowShot.put(uuid, currentTime);
    }

    private void checkFastClick(Player player, long currentTime) {
        UUID uuid = player.getUniqueId();
        Long lastTime = lastInteract.get(uuid);
        
        if (lastTime != null && currentTime - lastTime < 50) {
            fastclickVL.put(uuid, fastclickVL.getOrDefault(uuid, 0) + 1);
            if (fastclickVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "Interaction", "FastClick",
                    "Click delay: " + (currentTime - lastTime) + "ms",
                    plugin.getConfig().getInt("violations.fastclick", 15));
                fastclickVL.put(uuid, 0);
            }
        } else {
            fastclickVL.put(uuid, Math.max(0, fastclickVL.getOrDefault(uuid, 0) - 1));
        }
        lastInteract.put(uuid, currentTime);
    }

    private void checkFastBreak(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastBreak = lastBlockBreak.get(uuid);
        
        if (lastBreak != null && currentTime - lastBreak < 100) {
            fastbreakVL.put(uuid, fastbreakVL.getOrDefault(uuid, 0) + 1);
            if (fastbreakVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "World", "FastBreak",
                    "Break delay: " + (currentTime - lastBreak) + "ms",
                    plugin.getConfig().getInt("violations.fastbreak", 15));
                fastbreakVL.put(uuid, 0);
            }
        } else {
            fastbreakVL.put(uuid, Math.max(0, fastbreakVL.getOrDefault(uuid, 0) - 1));
        }
        lastBlockBreak.put(uuid, currentTime);
    }

    private void checkFastPlace(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastPlace = lastBlockPlace.get(uuid);
        
        if (lastPlace != null && currentTime - lastPlace < 50) {
            fastplaceVL.put(uuid, fastplaceVL.getOrDefault(uuid, 0) + 1);
            if (fastplaceVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "World", "FastPlace",
                    "Place delay: " + (currentTime - lastPlace) + "ms",
                    plugin.getConfig().getInt("violations.fastplace", 15));
                fastplaceVL.put(uuid, 0);
            }
        } else {
            fastplaceVL.put(uuid, Math.max(0, fastplaceVL.getOrDefault(uuid, 0) - 1));
        }
        lastBlockPlace.put(uuid, currentTime);
    }

    private void checkFastConsume(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastConsume = lastItemConsume.get(uuid);
        
        if (lastConsume != null && currentTime - lastConsume < 100) {
            fastconsumeVL.put(uuid, fastconsumeVL.getOrDefault(uuid, 0) + 1);
            if (fastconsumeVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "Interaction", "FastConsume",
                    "Consume delay: " + (currentTime - lastConsume) + "ms",
                    plugin.getConfig().getInt("violations.fastconsume", 10));
                fastconsumeVL.put(uuid, 0);
            }
        } else {
            fastconsumeVL.put(uuid, Math.max(0, fastconsumeVL.getOrDefault(uuid, 0) - 1));
        }
        lastItemConsume.put(uuid, currentTime);
    }

    private void checkFastDrop(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastDrop = lastItemDrop.get(uuid);
        
        if (lastDrop != null && currentTime - lastDrop < 50) {
            fastdropVL.put(uuid, fastdropVL.getOrDefault(uuid, 0) + 1);
            if (fastdropVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "Interaction", "FastDrop",
                    "Drop delay: " + (currentTime - lastDrop) + "ms",
                    plugin.getConfig().getInt("violations.fastdrop", 10));
                fastdropVL.put(uuid, 0);
            }
        } else {
            fastdropVL.put(uuid, Math.max(0, fastdropVL.getOrDefault(uuid, 0) - 1));
        }
        lastItemDrop.put(uuid, currentTime);
    }

    private void checkAutoEat(Player player) {
        UUID uuid = player.getUniqueId();
        if (player.getFoodLevel() < 20) {
            autoeatVL.put(uuid, autoeatVL.getOrDefault(uuid, 0) + 1);
            if (autoeatVL.getOrDefault(uuid, 0) >= 5) {
                plugin.getPunishmentManager().addViolation(player, "Interaction", "AutoEat",
                    "Suspicious eating pattern",
                    plugin.getConfig().getInt("violations.autoeat", 10));
                autoeatVL.put(uuid, 0);
            }
        }
    }

    private void checkDupe(Player player, InventoryClickEvent event) {
        UUID uuid = player.getUniqueId();
        ItemStack item = event.getCurrentItem();
        
        if (item != null && item.getAmount() > 64) {
            dupeVL.put(uuid, dupeVL.getOrDefault(uuid, 0) + 1);
            if (dupeVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Inventory", "Dupe",
                    "Item amount: " + item.getAmount(),
                    plugin.getConfig().getInt("violations.dupe", 10));
                dupeVL.put(uuid, 0);
            }
        }
    }

    private void checkGodMode(Player player) {
        UUID uuid = player.getUniqueId();
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double lastHealthVal = lastHealth.getOrDefault(uuid, maxHealth);
        
        if (health > lastHealthVal + 2.0) {
            godmodeVL.put(uuid, godmodeVL.getOrDefault(uuid, 0) + 1);
            if (godmodeVL.getOrDefault(uuid, 0) >= 3) {
                plugin.getPunishmentManager().addViolation(player, "Combat", "GodMode",
                    "Instant health regen",
                    plugin.getConfig().getInt("violations.godmode", 10));
                godmodeVL.put(uuid, 0);
            }
        } else {
            godmodeVL.put(uuid, Math.max(0, godmodeVL.getOrDefault(uuid, 0) - 1));
        }
        lastHealth.put(uuid, health);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Reset all data on join
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        lastY.remove(uuid);
        lastMoveTime.remove(uuid);
        airTicks.remove(uuid);
        lastHitTime.remove(uuid);
        lastBowShot.remove(uuid);
        lastInteract.remove(uuid);
        lastBlockBreak.remove(uuid);
        lastBlockPlace.remove(uuid);
        lastItemConsume.remove(uuid);
        lastItemDrop.remove(uuid);
        lastChat.remove(uuid);
        lastCommand.remove(uuid);
        blockPlaceHistory.remove(uuid);
        lastLocation.remove(uuid);
        lastHealth.remove(uuid);
        lastVelocityX.remove(uuid);
        lastVelocityZ.remove(uuid);
        
        speedVL.remove(uuid);
        flyVL.remove(uuid);
        nofallVL.remove(uuid);
        timerVL.remove(uuid);
        velocityVL.remove(uuid);
        criticalsVL.remove(uuid);
        aimbotVL.remove(uuid);
        triggerbotVL.remove(uuid);
        hitboxVL.remove(uuid);
        reachVL.remove(uuid);
        killauraVL.remove(uuid);
        bowauraVL.remove(uuid);
        fastclickVL.remove(uuid);
        fastbreakVL.remove(uuid);
        fastplaceVL.remove(uuid);
        fastconsumeVL.remove(uuid);
        fastdropVL.remove(uuid);
        phaseVL.remove(uuid);
        scaffoldVL.remove(uuid);
        jesusVL.remove(uuid);
        spiderVL.remove(uuid);
        stepVL.remove(uuid);
        godmodeVL.remove(uuid);
        dupeVL.remove(uuid);
        espVL.remove(uuid);
        autoeatVL.remove(uuid);
        spamVL.remove(uuid);
        advertiseVL.remove(uuid);
        swearingVL.remove(uuid);
    }
}