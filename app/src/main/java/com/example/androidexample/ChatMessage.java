package com.example.androidexample;

public class ChatMessage {
    public enum Kind { INCOMING, OUTGOING, SYSTEM }

    public final String sender;
    public final String text;
    public final Kind kind;
    public final long timestampMs;

    public ChatMessage(String sender, String text, Kind kind, Long timestampMs) {
        this.sender = sender;
        this.text = text;
        this.kind = kind;
        this.timestampMs = timestampMs == null ? System.currentTimeMillis() : timestampMs;
    }
}