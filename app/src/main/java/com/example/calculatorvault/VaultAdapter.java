package com.example.calculatorvault;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VaultAdapter extends RecyclerView.Adapter<VaultAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(VaultItem item);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(VaultItem item);
    }

    private final List<VaultItem> items;
    private final VaultStorage storage;
    private final OnItemClickListener listener;
    private final OnItemDeleteListener deleteListener;
    private final ExecutorService thumbnailExecutor = Executors.newSingleThreadExecutor();

    public VaultAdapter(
            List<VaultItem> items,
            VaultStorage storage,
            OnItemClickListener listener,
            OnItemDeleteListener deleteListener
    ) {
        this.items = items;
        this.storage = storage;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LinearLayout card = new LinearLayout(parent.getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(parent, 6);
        card.setPadding(padding, padding, padding, padding);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(245, 245, 245));
        background.setCornerRadius(dp(parent, 10));
        card.setBackground(background);

        ImageView preview = new ImageView(parent.getContext());
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackgroundColor(Color.LTGRAY);

        TextView title = new TextView(parent.getContext());
        title.setTextSize(14);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);

        TextView subtitle = new TextView(parent.getContext());
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);

        Button delete = new Button(parent.getContext());
        delete.setText("🗑️");
        delete.setContentDescription("Excluir arquivo");

        card.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(parent, 130)
        ));
        card.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        card.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        card.addView(delete, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        return new ViewHolder(card, preview, title, subtitle, delete);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        VaultItem item = items.get(position);

        holder.preview.setImageResource(android.R.drawable.ic_menu_gallery);
        holder.title.setText(item.getName());

        String type = item.isVideo() ? "🎬 Vídeo" : "🖼️ Foto";
        holder.subtitle.setText(type + " • " + formatSize(item.getSize()));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.delete.setOnClickListener(v -> deleteListener.onItemDelete(item));

        thumbnailExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                File temp = storage.createTemporaryDecryptedFile(
                        item.getFile(),
                        item.getMimeType()
                );

                if (item.isImage()) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    bitmap = BitmapFactory.decodeFile(temp.getAbsolutePath(), options);
                } else if (item.isVideo()) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(temp.getAbsolutePath());
                        bitmap = retriever.getFrameAtTime(
                                0,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        );
                    } finally {
                        retriever.release();
                    }
                }

                if (temp.exists()) temp.delete();
            } catch (Exception ignored) {
            }

            final Bitmap result = bitmap;
            holder.preview.post(() -> {
                if (result != null && holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    holder.preview.setImageBitmap(result);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private int dp(ViewGroup parent, int value) {
        return (int) (value * parent.getResources().getDisplayMetrics().density + 0.5f);
    }

    public void shutdown() {
        thumbnailExecutor.shutdownNow();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView preview;
        TextView title;
        TextView subtitle;
        Button delete;

        public ViewHolder(
                @NonNull View itemView,
                ImageView preview,
                TextView title,
                TextView subtitle,
                Button delete
        ) {
            super(itemView);
            this.preview = preview;
            this.title = title;
            this.subtitle = subtitle;
            this.delete = delete;
        }
    }
}
