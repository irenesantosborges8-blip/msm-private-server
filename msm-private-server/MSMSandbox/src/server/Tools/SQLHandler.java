package server.Tools;

import org.json.JSONArray;
import org.json.JSONObject;

import server.MainExtension;

public class SQLHandler {
    private String password;
    private String apiUrl;
    private LocalDatabase localDb;
    private boolean useLocal;

    public SQLHandler(String password) {
        this.password = password;
        this.apiUrl = MainExtension.config.getProperty("db.api_url", "https://riotlove.pythonanywhere.com/admin/exec_sql");
        String dbMode = MainExtension.config.getProperty("db.mode", "remote");
        this.useLocal = dbMode.equalsIgnoreCase("local");

        if (this.useLocal) {
            String dbPath = MainExtension.config.getProperty("db.local_path", Settings.ServerRoot + "/database");
            this.localDb = new LocalDatabase(dbPath);
        }
    }

    private String remoteCommand(String command) {
        JSONObject requestJson = new JSONObject();
        requestJson.put("password", this.password);
        requestJson.put("sql_command", command);
        return Util.PostRequest(this.apiUrl, requestJson.toString());
    }

    public String sendCommand(String command) {
        if (useLocal && localDb != null && localDb.isConnected()) {
            JSONObject result = localDb.query(command);
            return result.toString();
        }
        return remoteCommand(command);
    }

    public JSONObject query(String sql, Object... params) {
        if (useLocal && localDb != null && localDb.isConnected()) {
            return localDb.query(sql, params);
        }
        String escaped = sql;
        for (Object p : params) {
            if (p instanceof String) {
                escaped = escaped.replaceFirst("\\?", "'" + ((String) p).replace("'", "''") + "'");
            } else {
                escaped = escaped.replaceFirst("\\?", String.valueOf(p));
            }
        }
        return new JSONObject(remoteCommand(escaped));
    }

    public void close() {
        if (localDb != null) {
            localDb.close();
        }
    }
}
