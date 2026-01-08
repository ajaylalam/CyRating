package com.example.androidexample;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Handles both:
 *   - chat WebSocket:      ws://.../chat/{groupId}/{username}?userId=...&className=...
 *   - notification socket: ws://.../notifications?userId=...&className=...
 */
public class WebSocketService {

    public interface MessageListener {
        void onOpen();
        void onChat(ChatMessage message);
        void onTyping(String sender);
        void onSystem(String text, @Nullable Integer online);
        void onClosed(int code, String reason);
        void onError(Throwable t);
    }

    private final OkHttpClient client = new OkHttpClient();

    private WebSocket chatSocket;
    private WebSocket notifSocket;
    private MessageListener listener;
    private String username;
    private String currentUser;

    /** Connect to both chat and notifications sockets. */
    public void connect(String classOrGroupId, String username, MessageListener listener) {
        // Close any old sockets first
        close();

        this.listener = listener;
        this.username = username;
        this.currentUser = username;

        // CHAT WebSocket
        Request chatReq = new Request.Builder()
                .url(ApiConfig.wsUrl(classOrGroupId, username))
                .build();
        chatSocket = client.newWebSocket(chatReq, new WsListener(false));

        // NOTIFICATIONS WebSocket (for @mentions)
        Request notifReq = new Request.Builder()
                .url(ApiConfig.wsNotifUrl(classOrGroupId, username))
                .build();
        notifSocket = client.newWebSocket(notifReq, new WsListener(true));
    }

    /** Send a normal chat message – backend expects plain text. */
    public void sendMessage(String text) {
        if (chatSocket != null) {
            chatSocket.send(text);
        }
    }

    /** Optional: send typing info. Backend will just ignore if it doesn't handle it. */
    public void sendTyping() {
        if (chatSocket == null) return;
        try {
            JSONObject o = new JSONObject();
            o.put("type", "TYPING");
            o.put("sender", username);
            chatSocket.send(o.toString());
        } catch (JSONException ignored) {
        }
    }

    /** Close sockets. */
    public void close() {
        if (chatSocket != null) {
            chatSocket.close(1000, "bye");
            chatSocket = null;
        }
        if (notifSocket != null) {
            notifSocket.close(1000, "bye");
            notifSocket = null;
        }
    }

    // ---------------------------------------------------------
    // Internal WebSocketListener that routes events to MessageListener
    // ---------------------------------------------------------
    private class WsListener extends WebSocketListener {
        private final boolean isNotifSocket;

        WsListener(boolean isNotifSocket) {
            this.isNotifSocket = isNotifSocket;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            if (!isNotifSocket && listener != null) {
                // Only call onOpen once (from the chat socket)
                listener.onOpen();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (listener == null) return;

            try {
                JSONObject obj = new JSONObject(text);
                String type = obj.optString("type", "");

                // ---- NOTIFICATIONS ----
                // For /notifications any text we receive is a system message.
                if (isNotifSocket || "NOTIF".equalsIgnoreCase(type)) {
                    String msg = obj.optString("message", text);
                    listener.onSystem(msg, null);
                    return;
                }

                // ---- GROUP CHAT MSG FORMAT ----
                // { "type":"MSG","groupId":"COMS311","id":115,"sender":"alice123","content":"hello", ... }
                if ("MSG".equalsIgnoreCase(type)) {
                    String sender = obj.optString("sender", "user");
                    String content = obj.optString("content", text);

                    ChatMessage.Kind kind = sender.equalsIgnoreCase(username)
                            ? ChatMessage.Kind.OUTGOING
                            : ChatMessage.Kind.INCOMING;

                    listener.onChat(new ChatMessage(sender, content, kind, null));
                    return;
                }

                // ---- SIMPLE CHAT FORMAT ----
                // { "from":"natethom", "message":"\"Hello everyone\"" }
                if (obj.has("from") && obj.has("message")) {
                    String from = obj.optString("from");
                    String msg = obj.optString("message");

                    // strip extra quotes if Postman added them
                    if (msg != null && msg.length() >= 2 &&
                            msg.startsWith("\"") && msg.endsWith("\"")) {
                        msg = msg.substring(1, msg.length() - 1);
                    }

                    ChatMessage.Kind kind = from.equalsIgnoreCase(username)
                            ? ChatMessage.Kind.OUTGOING
                            : ChatMessage.Kind.INCOMING;

                    listener.onChat(new ChatMessage(from, msg, kind, null));
                    return;
                }

                // ---- TYPING ----
                if ("TYPING".equalsIgnoreCase(type)) {
                    String sender = obj.optString("sender", null);
                    listener.onTyping(sender);
                    return;
                }

                // Anything else: treat as system text
                listener.onSystem(text, null);

            } catch (JSONException e) {
                // Not JSON – just show as system message
                listener.onSystem(text, null);
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            if (!isNotifSocket && listener != null) {
                listener.onClosed(code, reason);
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            if (listener != null) {
                listener.onError(t);
            }
        }
    }
}
