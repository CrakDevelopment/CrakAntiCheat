package com.crak.anticheat;

import com.crak.anticheat.integrations.DiscordIntegration;
import com.crak.anticheat.integrations.DatabaseManager;
import com.crak.anticheat.integrations.WebDashboard;
import com.crak.anticheat.utils.HostingDetector;
import com.crak.anticheat.utils.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    
    private CheckManager checkManager;
    private PunishmentManager punishmentManager;
    private AltDetector altDetector;
    private AntiXRay antiXRay;
    private AlertManager alertManager;
    private DiscordIntegration discordIntegration;
    private DatabaseManager databaseManager;
    private WebDashboard webDashboard;
    
    private boolean isPaper;
    private boolean isHostingService;
    private String hostingType;

    @Override
    public void onEnable() {
        instance = this;
        isPaper = VersionAdapter.isPaper();
        
        // Detect hosting environment
        HostingDetector.detectHosting();
        isHostingService = HostingDetector.isHostingService();
        hostingType = HostingDetector.getHostingName();
        
        // Save default config
        saveDefaultConfig();
        reloadConfig();
        
        // Initialize managers
        this.punishmentManager = new PunishmentManager(this);
        this.alertManager = new AlertManager(this);
        this.checkManager = new CheckManager(this);
        this.altDetector = new AltDetector(this);
        this.antiXRay = new AntiXRay(this);
        
        // Initialize integrations with hosting detection
        if (getConfig().getBoolean("discord.enabled", false)) {
            this.discordIntegration = new DiscordIntegration(this);
        }
        
        if (getConfig().getBoolean("database.enabled", false)) {
            this.databaseManager = new DatabaseManager(this);
        }
        
        // WEB DASHBOARD - AUTO-DISABLE ON HOSTING SERVICES
        boolean dashboardEnabled = getConfig().getBoolean("web-dashboard.enabled", false);
        if (dashboardEnabled && isHostingService) {
            getLogger().warning("=======================================");
            getLogger().warning("⚠️  WEB DASHBOARD DISABLED AUTO-DETECTED");
            getLogger().warning("   Detected: " + hostingType);
            getLogger().warning("   Web Dashboard requires local hosting!");
            getLogger().warning("   Use Discord webhook for alerts instead.");
            getLogger().warning("=======================================");
            getConfig().set("web-dashboard.enabled", false);
            saveConfig();
            webDashboard = null;
        } else if (dashboardEnabled && !isHostingService) {
            getLogger().info("✅ Web Dashboard enabled (Local Hosting detected)");
            this.webDashboard = new WebDashboard(this);
        } else {
            getLogger().info("ℹ️  Web Dashboard is disabled in config");
        }
        
        // Register events
        Bukkit.getPluginManager().registerEvents(checkManager, this);
        Bukkit.getPluginManager().registerEvents(altDetector, this);
        Bukkit.getPluginManager().registerEvents(antiXRay, this);
        
        // Register commands
        getCommand("crakanticheat").setExecutor(new AntiCheatCommand(this));
        getCommand("cac").setExecutor(new AntiCheatCommand(this));
        
        getLogger().info("=============================================");
        getLogger().info("  🛡️  Crak Anti-Cheat has been enabled!");
        getLogger().info("  Server Type: " + Bukkit.getServer().getName());
        getLogger().info("  Version: " + Bukkit.getServer().getVersion());
        getLogger().info("  Paper: " + isPaper);
        getLogger().info("  Hosting Type: " + hostingType);
        getLogger().info("  Web Dashboard: " + (webDashboard != null ? "✅ ENABLED" : "❌ DISABLED"));
        getLogger().info("  Protecting your server from hackers!");
        getLogger().info("=============================================");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
        if (webDashboard != null) webDashboard.stop();
        getLogger().info("Crak Anti-Cheat has been disabled!");
    }

    public static Main getInstance() { return instance; }
    public CheckManager getCheckManager() { return checkManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public AltDetector getAltDetector() { return altDetector; }
    public AntiXRay getAntiXRay() { return antiXRay; }
    public AlertManager getAlertManager() { return alertManager; }
    public DiscordIntegration getDiscordIntegration() { return discordIntegration; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public WebDashboard getWebDashboard() { return webDashboard; }
    public boolean isPaper() { return isPaper; }
    public boolean isHostingService() { return isHostingService; }
    public String getHostingType() { return hostingType; }
}