package com.crak.anticheat.integrations;

import com.crak.anticheat.Main;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class DiscordIntegration {
    private final Main plugin;
    private final String webhookUrl;
    private final boolean enabled;

    public DiscordIntegration(Main plugin) {
        this.plugin = plugin;
        this.webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        this.enabled = plugin.getConfig().getBoolean("discord.enabled", false);
    }

    public void sendAlert(Player player, String check, String details, int violations) {
        if (!enabled || webhookUrl.isEmpty()) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                String color = violations >= 10 ? "16711680" : "16776960";
                String json = String.format(
                    "{\"embeds\":[{" +
                    "\"title\":\"⚠️ Anti-Cheat Alert\"," +
                    "\"color\":%s," +
                    "\"fields\":[" +
                    "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," +
                    "{\"name\":\"Check\",\"value\":\"%s\",\"inline\":true}," +
                    "{\"name\":\"Violations\",\"value\":\"%d\",\"inline\":true}," +
                    "{\"name\":\"Details\",\"value\":\"%s\",\"inline\":false}" +
                    "]}]}",
                    color, player.getName(), check, violations, details
                );
                
                sendWebhook(json);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord alert: " + e.getMessage());
            }
        });
    }

    public void sendBan(Player player, String reason, String duration) {
        if (!enabled || webhookUrl.isEmpty()) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                String json = String.format(
                    "{\"embeds\":[{" +
                    "\"title\":\"🚫 Player Banned\"," +
                    "\"color\":16711680," +
                    "\"fields\":[" +
                    "{\"name\":\"Player\",\"value\":\"%s\",\"inline\":true}," +
                    "{\"name\":\"Reason\",\"value\":\"%s\",\"inline\":true}," +
                    "{\"name\":\"Duration\",\"value\":\"%s\",\"inline\":true}" +
                    "]}]}",
                    player.getName(), reason, duration
                );
                
                sendWebhook(json);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord ban: " + e.getMessage());
            }
        });
    }

    private void sendWebhook(String json) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            os.write(json.getBytes());
            os.flush();
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new RuntimeException("HTTP error: " + responseCode);
        }
        connection.disconnect();
    }
}