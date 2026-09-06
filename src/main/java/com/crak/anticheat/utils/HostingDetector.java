package com.crak.anticheat.utils;

import org.bukkit.Bukkit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

public class HostingDetector {
    private static HostingType detectedType = HostingType.UNKNOWN;
    private static boolean checked = false;

    public enum HostingType {
        LOCAL_HOST,      // Local PC or dedicated server
        ATERNOS,
        PLETHORA,        // Plethora (Aternos alternative)
        MINEHUT,
        SERVER_PRO,      // Server.pro
        SCALACUBE,
        BISECTHOSTING,
        PEBBLEHOST,
        SHOCKBYTE,
        OTHER_CLOUD,     // AWS, Google Cloud, etc.
        UNKNOWN
    }

    public static HostingType detectHosting() {
        if (checked) return detectedType;
        checked = true;

        try {
            // Method 1: Check system properties
            String osName = System.getProperty("os.name").toLowerCase();
            String userName = System.getProperty("user.name").toLowerCase();
            String hostName = InetAddress.getLocalHost().getHostName().toLowerCase();

            // Aternos detection
            if (userName.contains("aternos") || hostName.contains("aternos")) {
                detectedType = HostingType.ATERNOS;
                return detectedType;
            }

            // Plethora detection
            if (userName.contains("plethora") || hostName.contains("plethora")) {
                detectedType = HostingType.PLETHORA;
                return detectedType;
            }

            // Method 2: Check for hosting service indicators
            String env = System.getenv("HOSTNAME");
            if (env != null) {
                env = env.toLowerCase();
                if (env.contains("aternos")) {
                    detectedType = HostingType.ATERNOS;
                    return detectedType;
                }
                if (env.contains("minehut")) {
                    detectedType = HostingType.MINEHUT;
                    return detectedType;
                }
                if (env.contains("server.pro") || env.contains("serverpro")) {
                    detectedType = HostingType.SERVER_PRO;
                    return detectedType;
                }
                if (env.contains("scalacube")) {
                    detectedType = HostingType.SCALACUBE;
                    return detectedType;
                }
                if (env.contains("bisect")) {
                    detectedType = HostingType.BISECTHOSTING;
                    return detectedType;
                }
                if (env.contains("pebblehost")) {
                    detectedType = HostingType.PEBBLEHOST;
                    return detectedType;
                }
                if (env.contains("shockbyte")) {
                    detectedType = HostingType.SHOCKBYTE;
                    return detectedType;
                }
            }

            // Method 3: Check for cloud providers
            if (hostName.contains("aws") || hostName.contains("amazon") || 
                env != null && env.contains("aws")) {
                detectedType = HostingType.OTHER_CLOUD;
                return detectedType;
            }

            if (hostName.contains("gcp") || hostName.contains("google") || 
                env != null && env.contains("google")) {
                detectedType = HostingType.OTHER_CLOUD;
                return detectedType;
            }

            // Method 4: Check IP range for known hosting services
            String ip = getPublicIP();
            if (ip != null) {
                if (ip.startsWith("51.") || ip.startsWith("52.") || 
                    ip.startsWith("54.") || ip.startsWith("35.")) {
                    // AWS IP ranges
                    detectedType = HostingType.OTHER_CLOUD;
                    return detectedType;
                }
                // Add more IP range checks for other hosts
            }

            // Method 5: If not detected as hosting, assume local
            detectedType = HostingType.LOCAL_HOST;

        } catch (Exception e) {
            // If we can't detect, assume local
            detectedType = HostingType.LOCAL_HOST;
        }

        return detectedType;
    }

    private static String getPublicIP() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(whatismyip.openStream()))) {
                return in.readLine();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isHostingService() {
        return detectHosting() != HostingType.LOCAL_HOST;
    }

    public static boolean isAternos() {
        return detectHosting() == HostingType.ATERNOS;
    }

    public static String getHostingName() {
        HostingType type = detectHosting();
        switch (type) {
            case ATERNOS: return "Aternos";
            case PLETHORA: return "Plethora";
            case MINEHUT: return "Minehut";
            case SERVER_PRO: return "Server.pro";
            case SCALACUBE: return "Scalacube";
            case BISECTHOSTING: return "BisectHosting";
            case PEBBLEHOST: return "PebbleHost";
            case SHOCKBYTE: return "Shockbyte";
            case OTHER_CLOUD: return "Cloud Hosting";
            case LOCAL_HOST: return "Local/Dedicated Server";
            default: return "Unknown";
        }
    }

    public static boolean supportsWebDashboard() {
        return detectHosting() == HostingType.LOCAL_HOST;
    }

    public static boolean supportsMySQL() {
        // Most hosting services allow external MySQL
        return true;
    }
}