package com.example.calculatorvault;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class VaultAdapter
        extends RecyclerView.Adapter<VaultAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(VaultItem item);
    }

    private final List<VaultItem> items;
    private final OnItemClickListener listener;

    public VaultAdapter(
            List<VaultItem> items,
            OnItemClickListener listener
    ) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                android.R.layout.simple_list_item_2,
                                parent,
                                false
                        );

        return new ViewHolder(view);
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

        holder.subtitle.setText(
                type + " • " +
                formatSize(item.getSize())
        );

        holder.itemView.setOnClickListener(
                v -> listener.onItemClick(item)
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatSize(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format(
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        return String.format(
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView title;
        TextView subtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title =
                    itemView.findViewById(
                            android.R.id.text1
                    );

            subtitle =
                    itemView.findViewById(
                            android.R.id.text2
                    );
        }
    }
}