package com.example.calculatorvault;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VaultActivity extends AppCompatActivity {

    private VaultType vaultType;
    private VaultStorage storage;

    private RecyclerView recyclerView;
    private TextView storageInfo;

    private final List<VaultItem> items =
            new ArrayList<>();

    private ActivityResultLauncher<String[]> filePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String type =
                getIntent().getStringExtra("VAULT_TYPE");

        if ("FAKE".equals(type)) {
            vaultType = VaultType.FAKE;
        } else {
            vaultType = VaultType.REAL;
        }

        storage = new VaultStorage(this);

        createFilePicker();

        showVault();
        loadFiles();
    }

    private void createFilePicker() {

        filePicker =
                registerForActivityResult(
                        new ActivityResultContracts.OpenMultipleDocuments(),
                        uris -> {

                            if (uris == null) {
                                return;
                            }

                            for (Uri uri : uris) {

                                try {

                                    storage.importFile(
                                            uri,
                                            vaultType
                                    );

                                } catch (Exception e) {

                                    Toast.makeText(
                                            this,
                                            "Erro ao importar arquivo.",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }

                            loadFiles();
                        }
                );
    }

    private void showVault() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(16),
                dp(24),
                dp(16),
                dp(16)
        );

        TextView title =
                new TextView(this);

        title.setText(
                vaultType == VaultType.REAL
                        ? "🔒 Cofre privado"
                        : "📁 Cofre"
        );

        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        root.addView(title);

        storageInfo =
                new TextView(this);

        storageInfo.setTextSize(16);
        storageInfo.setPadding(
                0,
                dp(12),
                0,
                dp(12)
        );

        root.addView(storageInfo);

        Button add =
                new Button(this);

        add.setText(
                "Adicionar fotos/vídeos"
        );

        add.setOnClickListener(
                v -> filePicker.launch(
                        new String[]{
                                "image/*",
                                "video/*"
                        }
                )
        );

        root.addView(add);

        recyclerView =
                new RecyclerView(this);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        VaultAdapter adapter =
                new VaultAdapter(
                        items,
                        this::openItem
                );

        recyclerView.setAdapter(adapter);

        root.addView(
                recyclerView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        Button lock =
                new Button(this);

        lock.setText("🔒 Bloquear cofre");

        lock.setOnClickListener(
                v -> finish()
        );

        root.addView(lock);

        setContentView(root);
    }

    private void loadFiles() {

        items.clear();

        List<File> files =
                storage.getFiles(vaultType);

        for (File file : files) {

            String name =
                    file.getName();

            items.add(
                    new VaultItem(
                            file,
                            name,
                            "application/octet-stream",
                            file.length(),
                            file.lastModified()
                    )
            );
        }

        if (recyclerView != null
                && recyclerView.getAdapter() != null) {

            recyclerView
                    .getAdapter()
                    .notifyDataSetChanged();
        }

        updateStorageInfo();
    }

    private void updateStorageInfo() {

        long used =
                storage.getStorageUsed(
                        vaultType
                );

        storageInfo.setText(
                "Arquivos: " +
                items.size() +
                "\nEspaço usado: " +
                formatSize(used)
        );
    }

    private void openItem(
            VaultItem item
    ) {

        Toast.makeText(
                this,
                "Visualização será adicionada na próxima etapa.",
                Toast.LENGTH_SHORT
        ).show();
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

    private int dp(int value) {

        return (int) (
                value *
                getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }
}