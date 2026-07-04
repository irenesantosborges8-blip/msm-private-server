package server;

import java.io.File;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import server.Tools.Util;

public class Settings {
    public static String ServerRoot;

    public static int QUEUE = 0;
    public static int max_user_count = 100;

    static MainExtension ext;
    
    public static int[] brokenIslands = new int[] {};

    static {
        String osName = System.getProperty("os.name");
        String userDir = System.getProperty("user.dir");
        if (osName != null && osName.toLowerCase().contains("linux")) {
            ServerRoot = "/home/ubuntu/MSMSandbox/ServerData";
        } else {
            String localProp = userDir + "/local.properties";
            java.io.File propFile = new java.io.File(localProp);
            if (propFile.exists()) {
                try {
                    java.util.Properties props = new java.util.Properties();
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(propFile)) {
                        props.load(fis);
                    }
                    String customRoot = props.getProperty("server.root");
                    if (customRoot != null && !customRoot.isEmpty()) {
                        ServerRoot = customRoot;
                        return;
                    }
                } catch (Exception e) {
                    // fallback
                }
            }
            ServerRoot = userDir + "/ServerData";
        }
    }

    public static String getJsonDb() {
        String path = ServerRoot + "/json_db/Settings.json";
        File f = new File(path);
        if (f.exists()) return path;
        String userDir = System.getProperty("user.dir");
        String alt = userDir + "/Settings.json";
        File altF = new File(alt);
        if (altF.exists()) return alt;
        return path;
    }

    public static void setExtension(MainExtension extension) {
        ext = extension;
    }

    public static void logServerRoot() {
        if (ext != null) ext.trace("Settings ServerRoot is: " + ServerRoot);
    }

    public static String get(String settingName) {
        String path = getJsonDb();
        File file = new File(path);

        if (!file.exists()) {
            if (ext != null) ext.trace("Settings file does not exist: " + path);
            return "S";
        }

        String contents = Util.ReadFile(file);

        if (ext != null) ext.trace("Settings.json content: [" + contents + "]");

        if (contents == null || contents.isEmpty()) {
            if (ext != null) ext.trace("Settings file is empty or unreadable: " + path);
            return "S";
        }

        try {
            Gson gson = new Gson();
            JsonObject jsonObj = gson.fromJson(contents, JsonObject.class);

            if (ext != null) ext.trace("JSON parsed successfully.");

            if (jsonObj != null && jsonObj.has(settingName)) {
                return jsonObj.get(settingName).getAsString();
            } else {
                if (ext != null) ext.trace("Setting '" + settingName + "' not found in Settings.json");
                return "S";
            }
        } catch (JsonSyntaxException e) {
            if (ext != null) ext.trace("JSON syntax error: " + e.getMessage());
            throw e;  // rethrow for visibility in logs
        } catch (Exception e) {
            if (ext != null) ext.trace("Unexpected error parsing Settings.json: " + e.toString());
            throw e;
        }
    }

}
