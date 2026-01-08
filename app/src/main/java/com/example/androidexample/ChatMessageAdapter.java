package com.example.androidexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VT_IN = 1, VT_OUT = 2, VT_SYS = 3;

    private final List<ChatMessage> data = new ArrayList<>();

    public void add(ChatMessage m) {
        data.add(m);
        notifyItemInserted(data.size() - 1);
    }

    // used when we load history
    public void setMessages(List<ChatMessage> messages) {
        data.clear();
        if (messages != null) {
            data.addAll(messages);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = data.get(position);
        if (m.kind == ChatMessage.Kind.SYSTEM) return VT_SYS;
        return m.kind == ChatMessage.Kind.OUTGOING ? VT_OUT : VT_IN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());

        if (viewType == VT_SYS) {
            TextView tv = new TextView(parent.getContext());
            return new SysHolder(tv);
        }

        // simple 2-line layout: line 1 = name (Me / username), line 2 = message
        View v = inf.inflate(android.R.layout.simple_list_item_2, parent, false);
        boolean isOut = (viewType == VT_OUT);
        return new Holder(v, isOut);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage m = data.get(position);

        if (holder instanceof SysHolder) {
            ((SysHolder) holder).t.setText(m.text);
        } else if (holder instanceof Holder) {
            Holder h = (Holder) holder;

            // message text on second line
            h.subtitle.setText(m.text);

            // 👇 THIS is the important part:
            // Outgoing -> "Me"
            // Incoming -> actual sender name from server
            if (m.kind == ChatMessage.Kind.OUTGOING) {
                h.title.setText("Me");
            } else {
                String name = (m.sender == null || m.sender.isEmpty())
                        ? "User"
                        : m.sender;
                h.title.setText(name);
            }
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView title, subtitle;

        Holder(View v, boolean isOut) {
            super(v);
            title = v.findViewById(android.R.id.text1);
            subtitle = v.findViewById(android.R.id.text2);
        }
    }

    static class SysHolder extends RecyclerView.ViewHolder {
        TextView t;
        SysHolder(View v) {
            super(v);
            t = (TextView) v;
        }
    }
}
