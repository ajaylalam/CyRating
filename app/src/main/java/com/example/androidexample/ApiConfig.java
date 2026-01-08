package com.example.androidexample;

public final class ApiConfig {

    public static final String BASE_HTTP = "http://coms-3090-019.class.las.iastate.edu:8080";
    public static final String BASE_WS   = "ws://coms-3090-019.class.las.iastate.edu:8080";

    // REST history endpoint
    public static String historyUrl(String groupId, int size, int page) {
        return BASE_HTTP + "/api/chat/history/" + enc(groupId)
                + "?size=" + size + "&page=" + page;
    }

    // Chat websocket: ws://.../chat/COMS309/ajay123?userId=ajay123&className=COMS309
    public static String wsUrl(String groupId, String username) {
        return BASE_WS + "/chat/" + enc(groupId) + "/" + enc(username)
                + "?userId=" + enc(username) + "&className=" + enc(groupId);
    }

    // Notifications websocket: ws://.../notifications?userId=ajay123&className=COMS309
    public static String wsNotifUrl(String groupId, String username) {
        return BASE_WS + "/notifications"
                + "?userId=" + enc(username) + "&className=" + enc(groupId);
    }

    private static String enc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private ApiConfig() {}  // no instances
}
