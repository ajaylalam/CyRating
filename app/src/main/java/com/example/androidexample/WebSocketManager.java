package com.example.androidexample;

import android.content.Context;
import android.util.Log;

import java.net.URI;

public class WebSocketManager {
    private static WebSocketManager instance;
    private NotificationWebSocketClient wsClient;

    private WebSocketManager() {}

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    public void connect(String username, Context context) {
        try {
            if (wsClient == null || !wsClient.isOpen()) {
                URI uri = new URI("ws://coms-3090-019.class.las.iastate.edu:8080/ws-notifications?userId=" + username);
                wsClient = new NotificationWebSocketClient(uri, context);
                wsClient.connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(message);
        } else {
            Log.e("WebSocketManager", "WebSocket not connected!");
        }
    }
}