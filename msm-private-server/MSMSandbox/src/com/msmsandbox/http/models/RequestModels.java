package com.msmsandbox.http;

public class AuthRequest {
    public String username;
    public String password;
    public String loginType;
}

public class PregameSetupRequest {
    public String accessKey;
    public String clientVersion;
    public String deviceId;
    public String deviceModel;
    public String deviceVendor;
    public String lang;
    public String osVersion;
    public String advertiserId = "";
    public String authVersion = "2.0.0";
    public String packageName = "";
    public String platform = "pc";
}

public class DatabaseRequest {
    public SFSObject params;
}

public class GameStateRequest {
    public SFSObject params;
}

public class BattleRequest {
    public SFSObject params;
}

public class ResponseResult {
    public boolean success;
    public SFSObject data = new SFSObject();
}

public class HealthResponse {
    public String status = "healthy";
    public int connections;
    public long timestamp;
}

public class StatsResponse {
    public int activeConnections;
    public long totalRequests;
    public long serverStartTime;
}
