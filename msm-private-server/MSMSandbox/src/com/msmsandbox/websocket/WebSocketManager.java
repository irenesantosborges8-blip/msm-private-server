import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import spark.Request;
import spark.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class WebSocketManager {
    private static WebSocketManager instance;
    private WebSocketServer webSocketServer;
    private ScheduledExecutorService heartbeatScheduler;
    private Map<String, ClientConnection> activeConnections;
    private Queue<PendingMessage> messageQueue;
    private AtomicInteger connectionIdCounter;
    
    private WebSocketManager() {
        this.activeConnections = new HashMap<>();
        this.messageQueue = new LinkedList<>();
        this.connectionIdCounter = new AtomicInteger(1);
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        startHeartbeats();
    }
    
    public static WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }
    
    public void setWebSocketServer(WebSocketServer server) {
        this.webSocketServer = server;
    }
    
    public String addConnection(String userGameId, String sessionId, Map<String, Object> properties) {
        String connectionId = "conn_" + connectionIdCounter.incrementAndGet();
        ClientConnection conn = new ClientConnection(connectionId, userGameId, sessionId, properties);
        activeConnections.put(connectionId, conn);
        webSocketServer.broadcastConnectionEvent("client_connected", conn);
        return connectionId;
    }
    
    public boolean removeConnection(String connectionId) {
        ClientConnection conn = activeConnections.remove(connectionId);
        if (conn != null) {
            webSocketServer.broadcastConnectionEvent("client_disconnected", conn);
            return true;
        }
        return false;
    }
    
    public void registerConnection(String connectionId, String sessionId) {
        ClientConnection conn = activeConnections.get(connectionId);
        if (conn != null) {
            conn.setSessionId(sessionId);
            conn.setLastActive(System.currentTimeMillis());
        }
    }
    
    public void registerMessage(String connectionId, String message) {
        ClientConnection conn = activeConnections.get(connectionId);
        if (conn != null) {
            conn.setLastActive(System.currentTimeMillis());
        }
    }
    
    public void addMessage(Message message) {
        messageQueue.offer(message);
        processMessageQueue();
    }
    
    public ClientConnection getConnection(String connectionId) {
        return activeConnections.get(connectionId);
    }
    
    public Map<String, ClientConnection> getAllConnections() {
        return new HashMap<>(activeConnections);
    }
    
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
    
    public void cleanupInactiveConnections(long timeout) {
        long now = System.currentTimeMillis();
        activeConnections.entrySet().removeIf(entry -> {
            ClientConnection conn = entry.getValue();
            return conn.isInactive(now, timeout);
        });
    }
    
    private void processMessageQueue() {
        while (!messageQueue.isEmpty() && activeConnections.size() > 0) {
            PendingMessage pending = messageQueue.poll();
            ClientConnection targetConn = activeConnections.get(pending.connectionId);
            if (targetConn != null && targetConn.getSessionId() != null) {
                webSocketServer.sendToClient(targetConn.getSessionId(), pending.message);
            }
        }
    }
    
    public void startHeartbeats() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupInactiveConnections(60000);
                sendHeartbeatToClients();
            } catch (Exception e) {
                MainExtension.trace("WebSocket heartbeat error: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    public void sendHeartbeatToClients() {
        long now = System.currentTimeMillis();
        activeConnections.values().forEach(conn -> {
            if (conn.isInactive(now, 90000)) {
                removeConnection(conn.getConnectionId());
            } else {
                webSocketServer.sendToClient(conn.getSessionId(), getHeartbeatMessage());
            }
        });
    }
    
    public void shutdown() {
        heartbeatScheduler.shutdownNow();
        activeConnections.values().forEach(conn -> {
            if (conn.getSessionId() != null) {
                webSocketServer.sendToClient(conn.getSessionId(), "{" + "type":"disconnect","reason":"server_shutdown" + "}");
            }
        });
        activeConnections.clear();
    }
    
    public static class ClientConnection {
        private final String connectionId;
        private final String userGameId;
        private String sessionId;
        private final Map<String, Object> properties;
        private long lastActive;
        
        public ClientConnection(String connectionId, String userGameId, String sessionId, Map<String, Object> properties) {
            this.connectionId = connectionId;
            this.userGameId = userGameId;
            this.sessionId = sessionId;
            this.properties = properties;
            this.lastActive = System.currentTimeMillis();
        }
        
        public String getConnectionId() { return connectionId; }
        public String getUserGameId() { return userGameId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Map<String, Object> getProperties() { return properties; }
        public long getLastActive() { return lastActive; }
        public void setLastActive(long lastActive) { this.lastActive = lastActive; }
        public boolean isInactive(long now, long timeoutMs) {
            return (now - lastActive) > timeoutMs;
        }
        
        public Object getProperty(String key) { return properties.get(key); }
        public void setProperty(String key, Object value) { properties.put(key, value); }
        public String getStringProperty(String key) { return (String) properties.get(key); }
        public Long getLongProperty(String key) { return (Long) properties.get(key); }
        public Integer getIntProperty(String key) { return (Integer) properties.get(key); }
    }
    
    public static class Message {
        private final String connectionId;
        private final String message;
        private final long timestamp;
        
        public Message(String connectionId, String message) {
            this.connectionId = connectionId;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getConnectionId() { return connectionId; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class PendingMessage {
        private final String connectionId;
        private final String message;
        
        public PendingMessage(String connectionId, String message) {
            this.connectionId = connectionId;
            this.message = message;
        }
        
        public String getConnectionId() { return connectionId; }
        public String getMessage() { return message; }
    }
}
