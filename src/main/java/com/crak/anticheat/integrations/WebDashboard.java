package com.crak.anticheat.integrations;

import com.crak.anticheat.Main;
import com.crak.anticheat.utils.HostingDetector;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.BanList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WebDashboard {
    private final Main plugin;
    private HttpServer server;
    private final int port;
    private final String password;
    private final boolean enabled;
    private final boolean isHostingService;
    private final long startTime;  // ADDED THIS
    
    // Rate limiting
    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAttempt = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long ATTEMPT_TIMEOUT = 60000;

    // Session management
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 3600000;

    public WebDashboard(Main plugin) {
        this.plugin = plugin;
        this.startTime = System.currentTimeMillis();  // INITIALIZE HERE
        
        // Check if hosting service supports web dashboard
        this.isHostingService = HostingDetector.isHostingService();
        
        if (isHostingService) {
            plugin.getLogger().warning("=======================================");
            plugin.getLogger().warning("⚠️  WEB DASHBOARD DISABLED AUTO-DETECTED");
            plugin.getLogger().warning("   Detected: " + HostingDetector.getHostingName());
            plugin.getLogger().warning("   Web Dashboard requires local hosting!");
            plugin.getLogger().warning("   Use Discord webhook for alerts instead.");
            plugin.getLogger().warning("=======================================");
            this.enabled = false;
            this.port = 8080;
            this.password = "disabled";
            return;
        }
        
        this.enabled = plugin.getConfig().getBoolean("web-dashboard.enabled", false);
        this.port = plugin.getConfig().getInt("web-dashboard.port", 8080);
        this.password = plugin.getConfig().getString("web-dashboard.password", "admin123");
        
        if (password.equals("admin123") && enabled) {
            plugin.getLogger().warning("=======================================");
            plugin.getLogger().warning("⚠️  WARNING: Default password in use!");
            plugin.getLogger().warning("   Change 'web-dashboard.password' in");
            plugin.getLogger().warning("   config.yml to a secure password!");
            plugin.getLogger().warning("=======================================");
        }
        
        if (enabled) {
            if (!isPortAvailable(port)) {
                plugin.getLogger().severe("Port " + port + " is already in use!");
                plugin.getLogger().severe("Web Dashboard will NOT start!");
                return;
            }
            start();
        } else {
            plugin.getLogger().info("ℹ️  Web Dashboard is disabled in config");
        }
    }

    private boolean isPortAvailable(int port) {
        try {
            HttpServer testServer = HttpServer.create(new InetSocketAddress(port), 0);
            testServer.stop(0);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void start() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new DashboardHandler());
            server.createContext("/api", new ApiHandler());
            server.createContext("/api/alerts", new AlertsApiHandler());
            server.createContext("/api/bans", new BansApiHandler());
            server.createContext("/api/players", new PlayersApiHandler());
            server.createContext("/api/stats", new StatsApiHandler());
            server.createContext("/static", new StaticHandler());
            server.setExecutor(null);
            server.start();
            
            plugin.getLogger().info("========================================");
            plugin.getLogger().info("🌐 Web Dashboard started successfully!");
            plugin.getLogger().info("   Local Access: http://localhost:" + port);
            plugin.getLogger().info("   Network Access: http://" + hostAddress + ":" + port);
            plugin.getLogger().info("   Username: admin");
            plugin.getLogger().info("   Password: [hidden]");
            plugin.getLogger().info("========================================");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start web dashboard: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web Dashboard stopped.");
        }
    }

    public boolean isEnabled() {
        return enabled && !isHostingService && server != null;
    }

    // ============================================
    // AUTHENTICATION METHODS
    // ============================================

    private boolean isAuthorized(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        String cookie = headers.getFirst("Cookie");
        if (cookie != null) {
            String sessionId = extractSessionId(cookie);
            if (sessionId != null && sessions.containsKey(sessionId)) {
                long lastActivity = sessions.get(sessionId);
                if (System.currentTimeMillis() - lastActivity < SESSION_TIMEOUT) {
                    sessions.put(sessionId, System.currentTimeMillis());
                    return true;
                } else {
                    sessions.remove(sessionId);
                }
            }
        }

        String auth = headers.getFirst("Authorization");
        if (auth == null) return false;
        
        String[] parts = auth.split(" ");
        if (parts.length != 2 || !parts[0].equalsIgnoreCase("Basic")) return false;
        
        String decoded = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String[] credentials = decoded.split(":");
        if (credentials.length != 2) return false;
        
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (isRateLimited(ip)) {
            return false;
        }
        
        boolean valid = credentials[0].equals("admin") && credentials[1].equals(password);
        
        if (valid) {
            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, System.currentTimeMillis());
            exchange.getResponseHeaders().set("Set-Cookie", 
                "session=" + sessionId + "; Path=/; HttpOnly; Max-Age=3600");
            resetAttempts(ip);
        } else {
            recordFailedAttempt(ip);
        }
        
        return valid;
    }

    private String extractSessionId(String cookie) {
        for (String c : cookie.split(";")) {
            c = c.trim();
            if (c.startsWith("session=")) {
                return c.substring(8);
            }
        }
        return null;
    }

    private boolean isRateLimited(String ip) {
        if (!lastAttempt.containsKey(ip)) return false;
        long last = lastAttempt.get(ip);
        if (System.currentTimeMillis() - last > ATTEMPT_TIMEOUT) {
            resetAttempts(ip);
            return false;
        }
        int attempts = loginAttempts.getOrDefault(ip, 0);
        return attempts >= MAX_ATTEMPTS;
    }

    private void recordFailedAttempt(String ip) {
        loginAttempts.put(ip, loginAttempts.getOrDefault(ip, 0) + 1);
        lastAttempt.put(ip, System.currentTimeMillis());
    }

    private void resetAttempts(String ip) {
        loginAttempts.remove(ip);
        lastAttempt.remove(ip);
    }

    private void sendUnauthorized(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"Crak Anti-Cheat\"");
        sendResponse(exchange, "Unauthorized", "text/plain", 401);
    }

    private void sendResponse(HttpExchange exchange, String response, String contentType) throws IOException {
        sendResponse(exchange, response, contentType, 200);
    }

    private void sendResponse(HttpExchange exchange, String response, String contentType, int code) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        sendResponse(exchange, "", "text/html", 302);
    }

    // ============================================
    // HANDLER CLASSES
    // ============================================

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthorized(exchange)) {
                sendUnauthorized(exchange);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                sendResponse(exchange, generateDashboardHTML(), "text/html");
            } else if (path.equals("/login")) {
                sendResponse(exchange, generateLoginHTML(), "text/html");
            } else if (path.equals("/logout")) {
                String cookie = exchange.getRequestHeaders().getFirst("Cookie");
                if (cookie != null) {
                    String sessionId = extractSessionId(cookie);
                    if (sessionId != null) {
                        sessions.remove(sessionId);
                    }
                }
                exchange.getResponseHeaders().set("Set-Cookie", "session=; Path=/; Max-Age=0");
                sendRedirect(exchange, "/login");
            } else if (path.equals("/bans")) {
                sendResponse(exchange, generateBansHTML(), "text/html");
            } else if (path.equals("/players")) {
                sendResponse(exchange, generatePlayersHTML(), "text/html");
            } else if (path.equals("/alerts")) {
                sendResponse(exchange, generateAlertsHTML(), "text/html");
            } else if (path.equals("/settings")) {
                sendResponse(exchange, generateSettingsHTML(), "text/html");
            } else {
                sendResponse(exchange, "404 Not Found", "text/plain", 404);
            }
        }
    }

    private class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthorized(exchange)) {
                sendUnauthorized(exchange);
                return;
            }
            sendResponse(exchange, generateJSONStats(), "application/json");
        }
    }

    private class StatsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthorized(exchange)) {
                sendUnauthorized(exchange);
                return;
            }
            sendResponse(exchange, generateJSONStats(), "application/json");
        }
    }

    private class PlayersApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthorized(exchange)) {
                sendUnauthorized(exchange);
                return;
            }
            sendResponse(exchange, generateJSONPlayers(), "application/json");
        }
    }

    private class BansApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthorized(exchange)) {
                sendUnauthorized(exchange);
                return;
            }
            sendResponse(exchange, generateJSONBans(), "application/json");
        }
    }

    private class AlertsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthorized(exchange)) {
                sendUnauthorized(exchange);
                return;
            }
            sendResponse(exchange, generateJSONAlerts(), "application/json");
        }
    }

    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String resource = path.substring("/static/".length());
            
            try {
                InputStream is = getClass().getResourceAsStream("/web/" + resource);
                if (is == null) {
                    sendResponse(exchange, "404 Not Found", "text/plain", 404);
                    return;
                }
                
                String contentType = getContentType(resource);
                byte[] data = is.readAllBytes();
                is.close();
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            } catch (Exception e) {
                sendResponse(exchange, "404 Not Found", "text/plain", 404);
            }
        }

        private String getContentType(String filename) {
            if (filename.endsWith(".css")) return "text/css";
            if (filename.endsWith(".js")) return "application/javascript";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
            if (filename.endsWith(".gif")) return "image/gif";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            if (filename.endsWith(".ico")) return "image/x-icon";
            return "text/plain";
        }
    }

    // ============================================
    // HTML GENERATORS (Simplified)
    // ============================================

    private String generateDashboardHTML() {
        return "<!DOCTYPE html><html><head><title>Crak Anti-Cheat Dashboard</title>" +
               "<style>" +
               "body{font-family:Arial;background:#0a0a1a;color:#eee;margin:0;padding:0}" +
               ".header{background:linear-gradient(135deg,#1a1a3e,#2d1b69);padding:20px}" +
               ".header h1{margin:0;font-size:28px}.header h1 span{color:#00d4ff}" +
               ".nav{background:#1a1a2e;padding:10px 20px;border-bottom:2px solid #2d1b69}" +
               ".nav a{color:#aaa;text-decoration:none;padding:10px 20px;margin:0 5px;border-radius:5px}" +
               ".nav a:hover{background:#2d1b69;color:#fff}.nav a.active{background:#00d4ff;color:#000}" +
               ".container{max-width:1400px;margin:0 auto;padding:20px}" +
               ".stats-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:20px;margin-bottom:30px}" +
               ".stat-card{background:#1a1a3e;border-radius:12px;padding:20px;text-align:center;border:1px solid #2d1b69}" +
               ".stat-card .value{font-size:32px;font-weight:bold;color:#00d4ff}" +
               ".stat-card .label{font-size:14px;color:#888;margin-top:5px}" +
               ".stat-card .icon{font-size:40px;margin-bottom:10px}" +
               ".card{background:#1a1a3e;border-radius:12px;padding:20px;margin-bottom:20px;border:1px solid #2d1b69}" +
               ".card h2{margin-top:0;color:#00d4ff}" +
               ".table{width:100%;border-collapse:collapse}" +
               ".table th{background:#2d1b69;padding:12px;text-align:left}" +
               ".table td{padding:10px;border-bottom:1px solid #2d1b69}" +
               ".badge{padding:3px 10px;border-radius:12px;font-size:12px}" +
               ".badge-danger{background:#e74c3c;color:#fff}" +
               ".badge-warning{background:#f39c12;color:#fff}" +
               ".badge-success{background:#2ecc71;color:#fff}" +
               ".footer{text-align:center;padding:20px;color:#666;border-top:1px solid #2d1b69;margin-top:30px}" +
               "</style></head><body>" +
               "<div class='header'><h1>🛡️ Crak <span>Anti-Cheat</span> Dashboard</h1></div>" +
               "<div class='nav'>" +
               "<a href='/' class='active'>📊 Dashboard</a>" +
               "<a href='/players'>👥 Players</a>" +
               "<a href='/bans'>🚫 Bans</a>" +
               "<a href='/alerts'>⚠️ Alerts</a>" +
               "<a href='/settings'>⚙️ Settings</a>" +
               "<a href='/logout' style='float:right;'>🚪 Logout</a>" +
               "</div><div class='container'>" +
               "<div class='stats-grid'>" +
               "<div class='stat-card'><div class='icon'>👥</div><div class='value'>" + Bukkit.getOnlinePlayers().size() + "</div><div class='label'>Online Players</div></div>" +
               "<div class='stat-card'><div class='icon'>🚫</div><div class='value'>" + Bukkit.getBanList(BanList.Type.NAME).getBanEntries().size() + "</div><div class='label'>Total Bans</div></div>" +
               "<div class='stat-card'><div class='icon'>📦</div><div class='value'>" + Bukkit.getPluginManager().getPlugins().length + "</div><div class='label'>Plugins</div></div>" +
               "<div class='stat-card'><div class='icon'>🖥️</div><div class='value'>" + Bukkit.getServer().getName() + "</div><div class='label'>Server Type</div></div>" +
               "<div class='stat-card'><div class='icon'>📊</div><div class='value'>" + getUptime() + "</div><div class='label'>Uptime</div></div>" +
               "</div>" +
               "<div class='card'>" +
               "<h2>📊 Server Statistics</h2>" +
               "<p>Web Dashboard is running and monitoring your server!</p>" +
               "<p>Server Version: " + Bukkit.getServer().getVersion() + "</p>" +
               "<p>Hosting Type: " + plugin.getHostingType() + "</p>" +
               "<p>Paper: " + plugin.isPaper() + "</p>" +
               "</div>" +
               "<div class='footer'>Crak Anti-Cheat v" + plugin.getDescription().getVersion() + " | Protected by 🛡️</div>" +
               "</div></body></html>";
    }

    private String generateLoginHTML() {
        return "<!DOCTYPE html><html><head><title>Login - Crak Anti-Cheat</title>" +
               "<style>" +
               "body{font-family:Arial;background:#0a0a1a;color:#eee;display:flex;justify-content:center;align-items:center;height:100vh;margin:0}" +
               ".login-box{background:#1a1a3e;padding:40px;border-radius:12px;border:1px solid #2d1b69;width:350px}" +
               ".login-box h1{text-align:center;color:#00d4ff}" +
               ".login-box input{width:100%;padding:12px;margin:10px 0;border:1px solid #2d1b69;background:#0a0a1a;color:#eee;border-radius:5px}" +
               ".login-box button{width:100%;padding:12px;background:#00d4ff;color:#000;border:none;border-radius:5px;font-weight:bold;cursor:pointer}" +
               ".login-box button:hover{background:#00b8d4}" +
               "</style></head><body>" +
               "<div class='login-box'>" +
               "<h1>🛡️ Crak Anti-Cheat</h1>" +
               "<form method='POST' action='/'>" +
               "<input type='text' name='username' placeholder='Username' value='admin'>" +
               "<input type='password' name='password' placeholder='Password'>" +
               "<button type='submit'>Login</button>" +
               "</form></div></body></html>";
    }

    private String generateBansHTML() {
        String html = "<!DOCTYPE html><html><head><title>Bans - Crak Anti-Cheat</title>" +
               "<style>" +
               "body{font-family:Arial;background:#0a0a1a;color:#eee;margin:0;padding:0}" +
               ".header{background:linear-gradient(135deg,#1a1a3e,#2d1b69);padding:20px}" +
               ".header h1{margin:0;font-size:28px}.header h1 span{color:#00d4ff}" +
               ".nav{background:#1a1a2e;padding:10px 20px;border-bottom:2px solid #2d1b69}" +
               ".nav a{color:#aaa;text-decoration:none;padding:10px 20px;margin:0 5px;border-radius:5px}" +
               ".nav a:hover{background:#2d1b69;color:#fff}.nav a.active{background:#00d4ff;color:#000}" +
               ".container{max-width:1400px;margin:0 auto;padding:20px}" +
               ".card{background:#1a1a3e;border-radius:12px;padding:20px;border:1px solid #2d1b69}" +
               ".table{width:100%;border-collapse:collapse}" +
               ".table th{background:#2d1b69;padding:12px;text-align:left}" +
               ".table td{padding:10px;border-bottom:1px solid #2d1b69}" +
               "</style></head><body>" +
               "<div class='header'><h1>🚫 Crak <span>Anti-Cheat</span> Bans</h1></div>" +
               "<div class='nav'>" +
               "<a href='/'>📊 Dashboard</a>" +
               "<a href='/players'>👥 Players</a>" +
               "<a href='/bans' class='active'>🚫 Bans</a>" +
               "<a href='/alerts'>⚠️ Alerts</a>" +
               "<a href='/settings'>⚙️ Settings</a>" +
               "<a href='/logout' style='float:right;'>🚪 Logout</a>" +
               "</div><div class='container'><div class='card'><h2>📋 Ban List</h2>" +
               "<table class='table'><tr><th>Player</th><th>Reason</th><th>Source</th></tr>";
        
        for (org.bukkit.BanEntry entry : Bukkit.getBanList(BanList.Type.NAME).getBanEntries()) {
            html += "<tr><td>" + entry.getTarget() + "</td><td>" + entry.getReason() + "</td><td>" + entry.getSource() + "</td></tr>";
        }
        
        html += "</table></div></div></body></html>";
        return html;
    }

    private String generatePlayersHTML() {
        String html = "<!DOCTYPE html><html><head><title>Players - Crak Anti-Cheat</title>" +
               "<style>" +
               "body{font-family:Arial;background:#0a0a1a;color:#eee;margin:0;padding:0}" +
               ".header{background:linear-gradient(135deg,#1a1a3e,#2d1b69);padding:20px}" +
               ".header h1{margin:0;font-size:28px}.header h1 span{color:#00d4ff}" +
               ".nav{background:#1a1a2e;padding:10px 20px;border-bottom:2px solid #2d1b69}" +
               ".nav a{color:#aaa;text-decoration:none;padding:10px 20px;margin:0 5px;border-radius:5px}" +
               ".nav a:hover{background:#2d1b69;color:#fff}.nav a.active{background:#00d4ff;color:#000}" +
               ".container{max-width:1400px;margin:0 auto;padding:20px}" +
               ".card{background:#1a1a3e;border-radius:12px;padding:20px;border:1px solid #2d1b69}" +
               ".table{width:100%;border-collapse:collapse}" +
               ".table th{background:#2d1b69;padding:12px;text-align:left}" +
               ".table td{padding:10px;border-bottom:1px solid #2d1b69}" +
               "</style></head><body>" +
               "<div class='header'><h1>👥 Crak <span>Anti-Cheat</span> Players</h1></div>" +
               "<div class='nav'>" +
               "<a href='/'>📊 Dashboard</a>" +
               "<a href='/players' class='active'>👥 Players</a>" +
               "<a href='/bans'>🚫 Bans</a>" +
               "<a href='/alerts'>⚠️ Alerts</a>" +
               "<a href='/settings'>⚙️ Settings</a>" +
               "<a href='/logout' style='float:right;'>🚪 Logout</a>" +
               "</div><div class='container'><div class='card'><h2>👥 Online Players</h2>" +
               "<table class='table'><tr><th>Player</th><th>IP</th><th>Status</th></tr>";
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String ip = player.getAddress().getAddress().getHostAddress();
            String status = player.hasPermission("crakanticheat.bypass") ? 
                "<span class='badge badge-success'>Bypass</span>" : 
                "<span class='badge badge-warning'>Monitored</span>";
            html += "<tr><td>" + player.getName() + "</td><td>" + ip + "</td><td>" + status + "</td></tr>";
        }
        
        html += "</table></div></div></body></html>";
        return html;
    }

    private String generateAlertsHTML() {
        return "<!DOCTYPE html><html><head><title>Alerts - Crak Anti-Cheat</title>" +
               "<style>" +
               "body{font-family:Arial;background:#0a0a1a;color:#eee;margin:0;padding:0}" +
               ".header{background:linear-gradient(135deg,#1a1a3e,#2d1b69);padding:20px}" +
               ".header h1{margin:0;font-size:28px}.header h1 span{color:#00d4ff}" +
               ".nav{background:#1a1a2e;padding:10px 20px;border-bottom:2px solid #2d1b69}" +
               ".nav a{color:#aaa;text-decoration:none;padding:10px 20px;margin:0 5px;border-radius:5px}" +
               ".nav a:hover{background:#2d1b69;color:#fff}.nav a.active{background:#00d4ff;color:#000}" +
               ".container{max-width:1400px;margin:0 auto;padding:20px}" +
               ".card{background:#1a1a3e;border-radius:12px;padding:20px;border:1px solid #2d1b69}" +
               "</style></head><body>" +
               "<div class='header'><h1>⚠️ Crak <span>Anti-Cheat</span> Alerts</h1></div>" +
               "<div class='nav'>" +
               "<a href='/'>📊 Dashboard</a>" +
               "<a href='/players'>👥 Players</a>" +
               "<a href='/bans'>🚫 Bans</a>" +
               "<a href='/alerts' class='active'>⚠️ Alerts</a>" +
               "<a href='/settings'>⚙️ Settings</a>" +
               "<a href='/logout' style='float:right;'>🚪 Logout</a>" +
               "</div><div class='container'><div class='card'><h2>📋 Recent Alerts</h2>" +
               "<p>Alerts are sent to staff in-game and via Discord.</p>" +
               "<p>Check your console or Discord for recent alerts.</p>" +
               "</div></div></body></html>";
    }

    private String generateSettingsHTML() {
        return "<!DOCTYPE html><html><head><title>Settings - Crak Anti-Cheat</title>" +
               "<style>" +
               "body{font-family:Arial;background:#0a0a1a;color:#eee;margin:0;padding:0}" +
               ".header{background:linear-gradient(135deg,#1a1a3e,#2d1b69);padding:20px}" +
               ".header h1{margin:0;font-size:28px}.header h1 span{color:#00d4ff}" +
               ".nav{background:#1a1a2e;padding:10px 20px;border-bottom:2px solid #2d1b69}" +
               ".nav a{color:#aaa;text-decoration:none;padding:10px 20px;margin:0 5px;border-radius:5px}" +
               ".nav a:hover{background:#2d1b69;color:#fff}.nav a.active{background:#00d4ff;color:#000}" +
               ".container{max-width:1400px;margin:0 auto;padding:20px}" +
               ".card{background:#1a1a3e;border-radius:12px;padding:20px;border:1px solid #2d1b69}" +
               ".setting{padding:10px 0;border-bottom:1px solid #2d1b69}" +
               ".setting .key{color:#00d4ff;font-weight:bold}" +
               "</style></head><body>" +
               "<div class='header'><h1>⚙️ Crak <span>Anti-Cheat</span> Settings</h1></div>" +
               "<div class='nav'>" +
               "<a href='/'>📊 Dashboard</a>" +
               "<a href='/players'>👥 Players</a>" +
               "<a href='/bans'>🚫 Bans</a>" +
               "<a href='/alerts'>⚠️ Alerts</a>" +
               "<a href='/settings' class='active'>⚙️ Settings</a>" +
               "<a href='/logout' style='float:right;'>🚪 Logout</a>" +
               "</div><div class='container'><div class='card'><h2>📋 Server Settings</h2>" +
               "<div class='setting'><span class='key'>Server Name:</span> " + Bukkit.getServer().getName() + "</div>" +
               "<div class='setting'><span class='key'>Version:</span> " + Bukkit.getServer().getVersion() + "</div>" +
               "<div class='setting'><span class='key'>Paper:</span> " + plugin.isPaper() + "</div>" +
               "<div class='setting'><span class='key'>Hosting:</span> " + plugin.getHostingType() + "</div>" +
               "<div class='setting'><span class='key'>Web Dashboard:</span> " + (isEnabled() ? "✅ Enabled" : "❌ Disabled") + "</div>" +
               "<div class='setting'><span class='key'>Online Players:</span> " + Bukkit.getOnlinePlayers().size() + "</div>" +
               "<div class='setting'><span class='key'>Total Bans:</span> " + Bukkit.getBanList(BanList.Type.NAME).getBanEntries().size() + "</div>" +
               "<div class='setting'><span class='key'>Plugins:</span> " + Bukkit.getPluginManager().getPlugins().length + "</div>" +
               "</div></div></body></html>";
    }

    // ============================================
    // JSON GENERATORS
    // ============================================

    @SuppressWarnings("unchecked")
    private String generateJSONStats() {
        JSONObject json = new JSONObject();
        json.put("online_players", Bukkit.getOnlinePlayers().size());
        json.put("total_bans", Bukkit.getBanList(BanList.Type.NAME).getBanEntries().size());
        json.put("server_version", Bukkit.getServer().getVersion());
        json.put("server_type", Bukkit.getServer().getName());
        json.put("is_paper", plugin.isPaper());
        json.put("hosting_type", plugin.getHostingType());
        json.put("web_dashboard_enabled", isEnabled());
        json.put("uptime", getUptime());
        json.put("plugins", Bukkit.getPluginManager().getPlugins().length);
        return json.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private String generateJSONPlayers() {
        JSONArray players = new JSONArray();
        for (Player player : Bukkit.getOnlinePlayers()) {
            JSONObject p = new JSONObject();
            p.put("name", player.getName());
            p.put("uuid", player.getUniqueId().toString());
            p.put("ip", player.getAddress().getAddress().getHostAddress());
            p.put("world", player.getWorld().getName());
            p.put("health", player.getHealth());
            p.put("food", player.getFoodLevel());
            p.put("bypass", player.hasPermission("crakanticheat.bypass"));
            players.add(p);
        }
        return players.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private String generateJSONBans() {
        JSONArray bans = new JSONArray();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (org.bukkit.BanEntry entry : Bukkit.getBanList(BanList.Type.NAME).getBanEntries()) {
            JSONObject b = new JSONObject();
            b.put("player", entry.getTarget());
            b.put("reason", entry.getReason());
            b.put("source", entry.getSource());
            b.put("created", sdf.format(entry.getCreated()));
            bans.add(b);
        }
        return bans.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private String generateJSONAlerts() {
        JSONArray alerts = new JSONArray();
        // Simulated alerts - in real implementation would come from database
        JSONObject alert = new JSONObject();
        alert.put("time", new SimpleDateFormat("HH:mm:ss").format(new Date()));
        alert.put("player", "Example");
        alert.put("check", "Speed");
        alert.put("vl", "5");
        alert.put("details", "Distance: 0.856 blocks");
        alerts.add(alert);
        return alerts.toJSONString();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private String getUptime() {
        long uptime = System.currentTimeMillis() - startTime;  // FIXED - no getStartTime()
        long days = uptime / 86400000;
        long hours = (uptime % 86400000) / 3600000;
        long minutes = (uptime % 3600000) / 60000;
        
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}