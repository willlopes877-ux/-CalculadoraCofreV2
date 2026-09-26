package com.example.calculatorvault;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VaultAdapter
        extends RecyclerView.Adapter<VaultAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(VaultItem item);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(VaultItem item);
    }

    private final List<VaultItem> items;
    private final OnItemClickListener listener;
    private final OnItemDeleteListener deleteListener;

    public VaultAdapter(
            List<VaultItem> items,
            OnItemClickListener listener,
            OnItemDeleteListener deleteListener
    ) {
        this.items = items;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padding = dp(parent, 8);
        row.setPadding(padding, padding, padding, padding);

        LinearLayout textBox = new LinearLayout(parent.getContext());
        textBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(parent.getContext());
        title.setTextSize(16);
        title.setTextColor(Color.BLACK);

        TextView subtitle = new TextView(parent.getContext());
        subtitle.setTextSize(14);

        textBox.addView(title);
        textBox.addView(subtitle);

        Button delete = new Button(parent.getContext());
        delete.setText("🗑️");
        delete.setTextSize(18);
        delete.setContentDescription("Excluir arquivo");

        row.addView(textBox, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        row.addView(delete, new LinearLayout.LayoutParams(
                dp(parent, 64), ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        return new ViewHolder(row, title, subtitle, delete);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        VaultItem item = items.get(position);

        holder.title.setText(item.getName());

        String type;
        if (item.isVideo()) {
            type = "🎬 Vídeo";
        } else if (item.isImage()) {
            type = "🖼️ Foto";
        } else {
            type = "📁 Arquivo";
        }

        holder.subtitle.setText(type + " • " + formatSize(item.getSize()));

        holder.itemView.setOnClickListener(
                v -> listener.onItemClick(item)
        );

        holder.delete.setOnClickListener(
                v -> deleteListener.onItemDelete(item)
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private int dp(ViewGroup parent, int value) {
        return (int) (value * parent.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;
        Button delete;

        public ViewHolder(
                @NonNull View itemView,
                TextView title,
                TextView subtitle,
                Button delete
        ) {
            super(itemView);
            this.title = title;
            this.subtitle = subtitle;
            this.delete = delete;
        }
    }
}
