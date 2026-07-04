package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.json.JSONObject;

import server.Entities.Player;
import server.Tools.MSMClient;
import server.Tools.SQLHandler;
import server.Tools.Util;

public class MainExtension {
    public static Properties config = new Properties();
    
    public static String encryptionVector;
    public static String encryptionSecretKey;
    
    public static int sessionsSinceStart = 0;
    
    public static MSMClient client;
    public static SQLHandler sqlHandler;
    
    public static boolean can_play = false;
    public static String cant_play_reason = "Server reloading!";
    
    public static boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");

    public static long startTime = 0;

    public static com.smartfoxserver.v2.entities.data.SFSObject gameSettings = new com.smartfoxserver.v2.entities.data.SFSObject();

    public static final Object eggLock = new Object();
	
    public static final com.smartfoxserver.v2.entities.data.SFSObject allowedVersions = new com.smartfoxserver.v2.entities.data.SFSObject();
	
    public static com.smartfoxserver.v2.entities.data.SFSArray timedEventsCache = new com.smartfoxserver.v2.entities.data.SFSArray();

    public void cacheDbs() {
        can_play = false;
        cant_play_reason = "Server reloading!";

        String bbbUser = config.getProperty("bbb.username", "g92gtktd9wcj");
        String bbbPass = config.getProperty("bbb.password", "mj28v95q7xmvb4r5tkf8");
        String bbbLogin = config.getProperty("bbb.login_type", "anon");
        String bbbVer = config.getProperty("bbb.client_version", "4.8.4");
        String bbbKey = config.getProperty("bbb.access_key", allowedVersions.getUtfString("4.8.4"));

        client = new MSMClient(bbbUser, bbbPass, bbbLogin, bbbVer, bbbKey, true);

        try {
            String cacheDir = Settings.ServerRoot + "/data_cache";
            File cacheFile = new File(cacheDir + "/game_data.json");
            
            if (cacheFile.exists()) {
                trace("Loading game data from local cache...");
                String json = Util.ReadFile(cacheFile);
                if (json != null && !json.isEmpty()) {
                    client.loadFromCache(json);
                    trace("Local cache loaded successfully");
                    can_play = true;
                    return;
                }
            }

            com.smartfoxserver.v2.entities.data.SFSObject auth = client.auth();
            trace("Auth response: " + auth.toJson());

            if (auth.getBool("ok")) {
                com.smartfoxserver.v2.entities.data.SFSObject pregameSetup = client.pregameSetup();
                trace("Pregame Setup response: " + pregameSetup.toJson());

                if (!pregameSetup.getBool("ok")) {
                    client.server_ip = "3.142.208.146";
                }
                
                // New HTTP server will handle connection
                trace("Pregame setup complete, HTTP server will handle client connections");

                new java.util.Timer().schedule(new java.util.TimerTask() {
                    public void run() {
                        if (client.downloads.size() > 0) {
                            new File(cacheDir).mkdirs();
                            String cacheJson = client.saveToCache();
                            Util.WriteFile(cacheFile, cacheJson, false);
                            trace("Game data cached to " + cacheFile);
                        }
                        can_play = true;
                    }
                }, 5000);
            } else {
                trace("Auth failed: " + auth.getUtfString("message"));
                can_play = true;
            }
        } catch (Exception e) {
            trace("Error in cacheDbs: " + e.toString());
            can_play = true;
        }
    }
    
    private void loadConfig() {
        String userDir = System.getProperty("user.dir");
        String[] paths = {
            userDir + "/config.properties",
            userDir + "/../config.properties",
            Settings.ServerRoot + "/config.properties"
        };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                try (InputStream is = new FileInputStream(f)) {
                    config.load(is);
                    trace("Loaded config from " + path);
                    return;
                } catch (Exception e) {
                    trace("Error loading config: " + e.toString());
                }
            }
        }
        trace("No config.properties found, using defaults");
    }
    
    public void init() {
        Settings.logServerRoot();
        
        loadConfig();
        
        encryptionVector = config.getProperty("encryption.vector", Settings.get("encryption_vector"));
        encryptionSecretKey = config.getProperty("encryption.secret", Settings.get("encryption_secret_key"));

        startTime = Util.getUnixTime();
        
        allowedVersions.putUtfString("4.6.1", "193f6d49-4051-4adc-9949-fa4f4e9fd43a");
        allowedVersions.putUtfString("4.8.2", "70ba5d5d-d903-4587-93d6-655c4814844f");
        allowedVersions.putUtfString("4.8.4", "33cdd406-b5e0-4ebf-8891-2c28b84af2ea");
        
        String dbPassword = config.getProperty("db.password", "no");
        sqlHandler = new SQLHandler(dbPassword);
        
        com.msmsandbox.http.HttpServer.initialize();
        
        cacheDbs();
        
        trace("MSM Sandbox HTTP+WebSocket Server initialized");
    }
    
    public void destroy() {
        trace("Shutting down HTTP+WebSocket server...");
        com.msmsandbox.http.HttpServer.shutdown();
        com.msmsandbox.websocket.WebSocketManager.getInstance().shutdown();
        trace("HTTP+WebSocket server shut down");
    }
    
    public void trace(String message) {
        System.out.println("[MSM] " + message);
    }
}
