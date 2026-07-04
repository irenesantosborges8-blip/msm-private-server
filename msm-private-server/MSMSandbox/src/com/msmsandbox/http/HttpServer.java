package com.msmsandbox.http;

import spark.Request;
import spark.Response;
import spark.Spark;
import com.google.gson.Gson;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer {
    private static final Gson gson = new Gson();
    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    private static final ScheduledExecutorService connectionMonitor = Executors.newSingleThreadScheduledExecutor();
    private static Map<String, WebSocketConnection> activeConnections = new HashMap<>();
    
    public static void initialize() {
        Spark.port(8080);
        Spark.threadPool(50, 100, 0);
        
        setupRoutes();
        startConnectionMonitor();
    }
    
    private static void setupRoutes() {
        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });
        
        Spark.options("/(.*)", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            return new Object();
        });
        
        Spark.post("/auth", HttpServer::handleAuth);
        Spark.post("/pregame_setup", HttpServer::handlePregameSetup);
        Spark.post("/db_*(request, response) => handleDatabaseRequest(request, response));
        Spark.post("/gs_*(request, response) => handleGameStateRequest(request, response));
        Spark.post("/battle_*(request, response) => handleBattleRequest(request, response));
        Spark.post("/*", HttpServer::handleGenericRequest);
        
        Spark.get("/health", HttpServer::handleHealth);
        Spark.get("/stats", HttpServer::handleStats);
    }
    
    private static String handleAuth(Request request, Response response) {
        AuthRequest authRequest = gson.fromJson(request.body(), AuthRequest.class);
        
        SFSObject responseData = new SFSObject();
        
        try {
            if (authRequest.username == null || authRequest.password == null || authRequest.loginType == null) {
                responseData.putBool("ok", false);
                responseData.putUtfString("message", "Missing required auth parameters");
                return gson.toJson(responseData);
            }
            
            String tokenResponse = MainExtension.client.auth(authRequest.username, authRequest.password, authRequest.loginType);
            
            if (tokenResponse.containsKey("error")) {
                responseData.putBool("ok", false);
                responseData.putUtfString("message", tokenResponse.getString("error"));
            } else {
                responseData.putBool("ok", true);
                responseData.putUtfString("access_token", tokenResponse.getString("access_token"));
                responseData.putUtfString("user_game_id", tokenResponse.getJSONArray("user_game_id").get(0).toString());
            }
        } catch (Exception e) {
            responseData.putBool("ok", false);
            responseData.putUtfString("message", "Auth error: " + e.getMessage());
        }
        
        response.status(200);
        response.type("application/json");
        return gson.toJson(responseData);
    }
    
    private static String handlePregameSetup(Request request, Response response) {
        PregameSetupRequest setupRequest = gson.fromJson(request.body(), PregameSetupRequest.class);
        
        SFSObject responseData = new SFSObject();
        
        try {
            if (setupRequest.accessKey == null || setupRequest.clientVersion == null) {
                responseData.putBool("ok", false);
                responseData.putUtfString("message", "Missing required setup parameters");
                return gson.toJson(responseData);
            }
            
            String setupResponse = MainExtension.client.pregameSetup(
                setupRequest.accessKey,
                setupRequest.clientVersion,
                setupRequest.deviceId,
                setupRequest.deviceModel,
                setupRequest.deviceVendor,
                setupRequest.lang,
                setupRequest.osVersion
            );
            
            if (setupResponse.containsKey("error")) {
                responseData.putBool("ok", false);
                responseData.putUtfString("message", setupResponse.getString("error"));
            } else {
                responseData.putBool("ok", true);
                responseData.putUtfString("serverIp", setupResponse.getString("server_ip"));
                responseData.putUtfString("contentUrl", setupResponse.getString("content_url"));
            }
        } catch (Exception e) {
            responseData.putBool("ok", false);
            responseData.putUtfString("message", "Pregame setup error: " + e.getMessage());
        }
        
        response.status(200);
        response.type("application/json");
        return gson.toJson(responseData);
    }
    
    private static String handleDatabaseRequest(Request request, Response response) {
        String path = request.pathInfo().substring(1);
        String cmd = path.substring(path.indexOf("/") + 1);
        
        DatabaseRequest dbRequest = gson.fromJson(request.body(), DatabaseRequest.class);
        
        return processRequest(cmd, dbRequest.params, response);
    }
    
    private static String handleGameStateRequest(Request request, Response response) {
        String path = request.pathInfo().substring(1);
        String cmd = path.substring(path.indexOf("/") + 1);
        
        GameStateRequest gsRequest = gson.fromJson(request.body(), GameStateRequest.class);
        
        return processRequest(cmd, gsRequest.params, response);
    }
    
    private static String handleBattleRequest(Request request, Response response) {
        String path = request.pathInfo().substring(1);
        String cmd = path.substring(path.indexOf("/") + 1);
        
        BattleRequest battleRequest = gson.fromJson(request.body(), BattleRequest.class);
        
        return processRequest(cmd, battleRequest.params, response);
    }
    
    private static String handleGenericRequest(Request request, Response response) {
        SFSObject responseData = new SFSObject();
        responseData.putBool("ok", false);
        responseData.putUtfString("message", "Unknown endpoint");
        response.status(404);
        return gson.toJson(responseData);
    }
    
    private static String processRequest(String cmd, SFSObject params, Response response) {
        GameStateHandler handler = new GameStateHandler();
        
        try {
            ResponseResult result = handler.processRequest(cmd, params);
            
            if (result.success) {
                response.status(200);
            } else {
                response.status(400);
            }
            
            response.type("application/json");
            return gson.toJson(result.data);
        } catch (Exception e) {
            SFSObject errorResponse = new SFSObject();
            errorResponse.putBool("ok", false);
            errorResponse.putUtfString("message", "Processing error: " + e.getMessage());
            response.status(500);
            response.type("application/json");
            return gson.toJson(errorResponse);
        }
    }
    
    private static String handleHealth(Request request, Response response) {
        HealthResponse health = new HealthResponse();
        health.status = "healthy";
        health.connections = connectionCount.get();
        health.timestamp = System.currentTimeMillis() / 1000;
        
        response.type("application/json");
        return gson.toJson(health);
    }
    
    private static String handleStats(Request request, Response response) {
        StatsResponse stats = new StatsResponse();
        stats.activeConnections = connectionCount.get();
        stats.totalRequests = MainExtension.getTotalRequests();
        stats.serverStartTime = MainExtension.getStartTime();
        
        response.type("application/json");
        return gson.toJson(stats);
    }
    
    private static void startConnectionMonitor() {
        connectionMonitor.scheduleAtFixedRate(() -> {
            try {
                cleanupInactiveConnections();n
                reportHealth();
            } catch (Exception e) {
                ext.trace("Connection monitor error: " + e.getMessage());
            }
        }, 30, 60, TimeUnit.SECONDS);
    }
    
    private static void cleanupInactiveConnections() {
        long now = System.currentTimeMillis();
        activeConnections.entrySet().removeIf(entry -> {
            WebSocketConnection conn = entry.getValue();
            return conn.isInactive(now);
        });
    }
    
    private static void reportHealth() {
        int activeConnections = connectionCount.get();
        if (activeConnections > Settings.maxConnectionCount * 0.8) {
            ext.trace("High connection count alert: " + activeConnections);
        }
    }
    
    public static void shutdown() {
        connectionMonitor.shutdownNow();
        activeConnections.values().forEach(WebSocketConnection::close);
        Spark.stop();
    }
}
