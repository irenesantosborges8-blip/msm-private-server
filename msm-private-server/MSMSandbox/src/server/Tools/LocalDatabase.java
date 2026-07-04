package server.Tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import server.MainExtension;

public class LocalDatabase {

    private Connection conn;
    private boolean connected = false;

    public LocalDatabase(String dbPath) {
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:" + dbPath + "/msm-server;AUTO_SERVER=TRUE", "sa", "");
            connected = true;
            initSchema();
            MainExtension.trace("H2 database connected at " + dbPath);
        } catch (Exception e) {
            MainExtension.trace("H2 connection failed: " + e.toString());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private void initSchema() {
        execute(
            "CREATE TABLE IF NOT EXISTS players (" +
            "  id VARCHAR(64) PRIMARY KEY," +
            "  nickname VARCHAR(64) DEFAULT 'New Player'," +
            "  coins BIGINT DEFAULT 100000000," +
            "  diamonds BIGINT DEFAULT 1000000," +
            "  food BIGINT DEFAULT 99999999," +
            "  keys BIGINT DEFAULT 99999999," +
            "  starpower BIGINT DEFAULT 99999999," +
            "  relics BIGINT DEFAULT 99999999," +
            "  shards BIGINT DEFAULT 99999999," +
            "  xp BIGINT DEFAULT 99999999," +
            "  active_island BIGINT DEFAULT 1," +
            "  direct_place INT DEFAULT 0," +
            "  date_created BIGINT," +
            "  last_login BIGINT" +
            ")"
        );
        execute(
            "CREATE TABLE IF NOT EXISTS users (" +
            "  bbb_id BIGINT PRIMARY KEY," +
            "  user_game_id VARCHAR(64)," +
            "  username VARCHAR(64)," +
            "  password VARCHAR(64)" +
            ")"
        );
        execute(
            "CREATE TABLE IF NOT EXISTS player_islands (" +
            "  user_island_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "  user_game_id VARCHAR(64)," +
            "  island_id INT," +
            "  likes INT DEFAULT 0," +
            "  dislikes INT DEFAULT 0" +
            ")"
        );
        execute(
            "CREATE TABLE IF NOT EXISTS player_monsters (" +
            "  user_monster_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "  user_island_id BIGINT," +
            "  user_game_id VARCHAR(64)," +
            "  monster_id INT," +
            "  pos_x INT," +
            "  pos_y INT," +
            "  muted INT DEFAULT 0," +
            "  flip INT DEFAULT 0," +
            "  big INT DEFAULT 0," +
            "  name VARCHAR(64)," +
            "  date_created BIGINT" +
            ")"
        );
        execute(
            "CREATE TABLE IF NOT EXISTS player_structures (" +
            "  user_structure_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "  user_island_id BIGINT," +
            "  user_game_id VARCHAR(64)," +
            "  structure_id INT," +
            "  pos_x INT," +
            "  pos_y INT," +
            "  scale DOUBLE DEFAULT 1.0," +
            "  flip INT DEFAULT 0," +
            "  name VARCHAR(64)," +
            "  date_created BIGINT" +
            ")"
        );
        execute(
            "CREATE TABLE IF NOT EXISTS player_eggs (" +
            "  user_egg_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "  user_island_id BIGINT," +
            "  user_game_id VARCHAR(64)," +
            "  monster_id INT," +
            "  structure_id BIGINT" +
            ")"
        );
        execute(
            "CREATE TABLE IF NOT EXISTS user_friends (" +
            "  user_1 BIGINT," +
            "  user_2 BIGINT," +
            "  PRIMARY KEY (user_1, user_2)" +
            ")"
        );
    }

    public void execute(String sql) {
        if (!connected) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            MainExtension.trace("DB execute error: " + e.toString());
        }
    }

    public JSONObject query(String sql, Object... params) {
        JSONObject result = new JSONObject();
        JSONArray rows = new JSONArray();
        if (!connected) {
            result.put("result", rows);
            return result;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String) {
                    stmt.setString(i + 1, (String) params[i]);
                } else if (params[i] instanceof Long) {
                    stmt.setLong(i + 1, (Long) params[i]);
                } else if (params[i] instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) params[i]);
                } else if (params[i] instanceof Double) {
                    stmt.setDouble(i + 1, (Double) params[i]);
                } else if (params[i] instanceof Boolean) {
                    stmt.setBoolean(i + 1, (Boolean) params[i]);
                } else {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            if (sql.trim().toUpperCase().startsWith("SELECT") || sql.trim().toUpperCase().startsWith("EXISTS")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    while (rs.next()) {
                        JSONArray row = new JSONArray();
                        for (int c = 1; c <= cols; c++) {
                            Object val = rs.getObject(c);
                            if (val == null) {
                                row.put(JSONObject.NULL);
                            } else if (val instanceof Number) {
                                row.put(val);
                            } else {
                                row.put(val.toString());
                            }
                        }
                        rows.put(row);
                    }
                }
            } else {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            MainExtension.trace("DB query error: " + e.toString() + " | SQL: " + sql);
        }
        result.put("result", rows);
        return result;
    }

    public void update(String sql, Object... params) {
        query(sql, params);
    }

    public void close() {
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) { }
        }
    }
}
