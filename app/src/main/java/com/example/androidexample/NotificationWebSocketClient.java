package com.example.androidexample;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class NotificationWebSocketClient extends WebSocketClient {

    private final Context context;
    private static final String CHANNEL_ID = "notifications_channel";

    public NotificationWebSocketClient(URI serverUri, Context context) {
        super(serverUri);
        this.context = context;
        createNotificationChannel();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d("WebSocket", "Connected to server");
    }

    @Override
    public void onMessage(String message) {
        Log.d("WebSocket", "Message received: " + message);
        showNotification("New Notification", message);
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("WebSocket", "Connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.e("WebSocket", "Error: " + ex.getMessage(), ex);
    }

    private void showNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, NewHomeActivity.class); // or your homepage
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // add an icon in res/drawable
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}