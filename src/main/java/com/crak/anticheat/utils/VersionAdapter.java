package com.crak.anticheat.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;

public class VersionAdapter {
    private static String serverVersion;
    private static boolean isPaper;
    private static boolean isSpigot;
    private static boolean isBukkit;
    
    static {
        try {
            serverVersion = Bukkit.getServer().getClass().getPackage().getName()
                .split("\\.")[3];
            isPaper = Bukkit.getServer().getName().equalsIgnoreCase("Paper");
            isSpigot = Bukkit.getServer().getName().equalsIgnoreCase("Spigot");
            isBukkit = Bukkit.getServer().getName().equalsIgnoreCase("Bukkit");
        } catch (Exception e) {
            serverVersion = "v1_16_R3";
        }
    }
    
    public static boolean isPaper() { return isPaper; }
    public static boolean isSpigot() { return isSpigot; }
    public static boolean isBukkit() { return isBukkit; }
    
    public static String getServerVersion() {
        return serverVersion;
    }
    
    public static boolean isNewerThan(int major, int minor) {
        String version = serverVersion.replace("v", "").replace("_", "");
        if (version.length() < 4) return false;
        int serverMajor = Integer.parseInt(version.substring(0, 2));
        int serverMinor = Integer.parseInt(version.substring(2, 4));
        
        if (serverMajor > major) return true;
        if (serverMajor == major && serverMinor >= minor) return true;
        return false;
    }
    
    public static boolean isAtLeast(int major, int minor) {
        return isNewerThan(major - 1, minor - 1) || isNewerThan(major, minor - 1);
    }
    
    public static boolean isLegacy() {
        return !isAtLeast(1, 13);
    }
    
    public static boolean isAtLeast1_16() { return isAtLeast(1, 16); }
    public static boolean isAtLeast1_17() { return isAtLeast(1, 17); }
    public static boolean isAtLeast1_18() { return isAtLeast(1, 18); }
    public static boolean isAtLeast1_19() { return isAtLeast(1, 19); }
    public static boolean isAtLeast1_20() { return isAtLeast(1, 20); }
}