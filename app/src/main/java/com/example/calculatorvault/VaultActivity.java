package com.example.calculatorvault;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
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
    private final List<VaultItem> items = new ArrayList<>();
    private ActivityResultLauncher<String[]> filePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String type = getIntent().getStringExtra("VAULT_TYPE");
        vaultType = "FAKE".equals(type) ? VaultType.FAKE : VaultType.REAL;
        storage = new VaultStorage(this);

        createFilePicker();
        showVault();
        loadFiles();
    }

    private void createFilePicker() {
        filePicker = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris == null || uris.isEmpty()) return;

                    int imported = 0;
                    String lastError = null;

                    for (Uri uri : uris) {
                        try {
                            storage.importFile(uri, vaultType);
                            imported++;
                        } catch (Exception e) {
                            lastError = e.getMessage();
                        }
                    }

                    loadFiles();

                    if (imported > 0) {
                        Toast.makeText(this, imported + " arquivo(s) guardado(s) no cofre.", Toast.LENGTH_SHORT).show();
                    }
                    if (lastError != null) {
                        Toast.makeText(this, "Não foi possível importar: " + lastError, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void showVault() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText(vaultType == VaultType.REAL ? "🔒 Cofre 1" : "🔐 Cofre 2");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        storageInfo = new TextView(this);
        storageInfo.setTextSize(16);
        storageInfo.setPadding(0, dp(12), 0, dp(12));
        root.addView(storageInfo);

        Button add = new Button(this);
        add.setText("Adicionar fotos/vídeos");
        add.setOnClickListener(v -> filePicker.launch(new String[]{"image/*", "video/*"}));
        root.addView(add);

        Button changePin = new Button(this);
        changePin.setText("Alterar senha deste cofre");
        changePin.setOnClickListener(v -> showChangePin());
        root.addView(changePin);

        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new VaultAdapter(items, this::openItem));
        root.addView(recyclerView, new LinearLayout.LayoutParams(-1, 0, 1));

        Button lock = new Button(this);
        lock.setText("🔒 Bloquear cofre");
        lock.setOnClickListener(v -> finish());
        root.addView(lock);

        setContentView(root);
    }

    private void loadFiles() {
        items.clear();
        items.addAll(storage.getItems(vaultType));

        if (recyclerView != null && recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
        updateStorageInfo();
    }

    private void updateStorageInfo() {
        long used = storage.getStorageUsed(vaultType);
        storageInfo.setText("Arquivos: " + items.size() + "\nEspaço usado: " + formatSize(used));
    }

    private void openItem(VaultItem item) {
        try {
            File temp = storage.createTemporaryDecryptedFile(item.getFile(), item.getMimeType());
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    temp
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, item.getMimeType());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showChangePin() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        EditText current = pinInput("Senha atual");
        EditText next = pinInput("Nova senha");
        EditText confirm = pinInput("Confirme a nova senha");
        box.addView(current);
        box.addView(next);
        box.addView(confirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Alterar senha")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPin = current.getText().toString();
            String newPin = next.getText().toString();
            String confirmPin = confirm.getText().toString();
            PinManager pm = new PinManager(this);

            boolean currentOk = vaultType == VaultType.REAL
                    ? pm.verifyRealPin(oldPin)
                    : pm.verifyFakePin(oldPin);

            if (!currentOk) {
                Toast.makeText(this, "Senha atual incorreta.", Toast.LENGTH_LONG).show();
                return;
            }
            if (newPin.length() < 4 || !newPin.matches("\\d+")) {
                Toast.makeText(this, "A nova senha precisa ter pelo menos 4 números.", Toast.LENGTH_LONG).show();
                return;
            }
            if (!newPin.equals(confirmPin)) {
                Toast.makeText(this, "As novas senhas não conferem.", Toast.LENGTH_LONG).show();
                return;
            }

            boolean otherUsed = vaultType == VaultType.REAL
                    ? pm.verifyFakePin(newPin)
                    : pm.verifyRealPin(newPin);

            if (otherUsed) {
                Toast.makeText(this, "Escolha uma senha diferente da do outro cofre.", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                if (vaultType == VaultType.REAL) pm.setRealPin(newPin);
                else pm.setFakePin(newPin);
                dialog.dismiss();
                Toast.makeText(this, "Senha alterada com sucesso.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao alterar a senha.", Toast.LENGTH_LONG).show();
            }
        }));

        dialog.show();
    }

    private EditText pinInput(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return e;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (storage != null) storage.clearTemporaryFiles();
        super.onDestroy();
    }
}
