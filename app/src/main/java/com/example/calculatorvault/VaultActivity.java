package com.example.calculatorvault;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.*;

public class VaultActivity extends AppCompatActivity {

    private VaultType type;
    private VaultStorage storage;
    private RecyclerView list;
    private TextView info;
    private final List<VaultItem> items = new ArrayList<>();
    private ActivityResultLauncher<String[]> picker;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        type = "FAKE".equals(getIntent().getStringExtra("VAULT_TYPE"))
                ? VaultType.FAKE : VaultType.REAL;

        storage = new VaultStorage(this);
        storage.clearTemporaryFiles();

        picker = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null) {
                        for (Uri uri : uris) {
                            try {
                                storage.importFile(uri, type);
                            } catch (Exception e) {
                                Toast.makeText(this, "Erro ao importar arquivo.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        load();
                    }
                });

        build();
        load();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + .5f);
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(12));

        TextView title = new TextView(this);
        title.setText(type == VaultType.REAL ? "🔒 Cofre 1" : "🔐 Cofre 2");
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(55)));

        info = new TextView(this);
        info.setTextSize(15);
        info.setPadding(0, dp(8), 0, dp(12));
        root.addView(info);

        Button add = new Button(this);
        add.setText("＋ Adicionar fotos e vídeos");
        add.setOnClickListener(v -> picker.launch(new String[]{"image/*", "video/*"}));
        root.addView(add);

        list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new VaultAdapter(items, this::open));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));

        Button changePin = new Button(this);
        changePin.setText("🔑 Alterar PIN deste cofre");
        changePin.setOnClickListener(v -> changeCurrentPin());
        root.addView(changePin);

        Button lock = new Button(this);
        lock.setText("🔒 Bloquear cofre");
        lock.setOnClickListener(v -> finish());
        root.addView(lock);

        setContentView(root);
    }

    private void load() {
        items.clear();
        items.addAll(storage.getItems(type));
        if (list.getAdapter() != null) list.getAdapter().notifyDataSetChanged();
        info.setText("Arquivos: " + items.size()
                + "\nEspaço usado: " + size(storage.getStorageUsed(type)));
    }

    private void open(VaultItem item) {
        try {
            File f = storage.createTemporaryDecryptedFile(item.getFile(), item.getMimeType());
            Uri u = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    f
            );

            Intent i = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(u, item.getMimeType())
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir o arquivo.", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeCurrentPin() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        EditText pin = new EditText(this);
        pin.setHint("Novo PIN (mínimo 4 números)");
        pin.setInputType(2);
        box.addView(pin);

        EditText confirm = new EditText(this);
        confirm.setHint("Confirme o novo PIN");
        confirm.setInputType(2);
        box.addView(confirm);

        new AlertDialog.Builder(this)
                .setTitle("Alterar PIN do " + (type == VaultType.REAL ? "Cofre 1" : "Cofre 2"))
                .setMessage("O novo PIN substituirá o PIN atual deste cofre.")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String value = pin.getText().toString();
                    String check = confirm.getText().toString();

                    if (value.length() < 4) {
                        Toast.makeText(this, "O PIN precisa ter pelo menos 4 números.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!value.matches("\\d+")) {
                        Toast.makeText(this, "Use somente números.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!value.equals(check)) {
                        Toast.makeText(this, "Os PINs não conferem.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        PinManager pm = storagePinManager();
                        if (type == VaultType.REAL) {
                            pm.setRealPin(value);
                        } else {
                            pm.setFakePin(value);
                        }
                        Toast.makeText(this, "PIN alterado com sucesso.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Não foi possível alterar o PIN.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private PinManager storagePinManager() {
        return new PinManager(this);
    }

    @Override
    protected void onDestroy() {
        storage.clearTemporaryFiles();
        super.onDestroy();
    }

    private String size(long b) {
        if (b < 1024) return b + " B";
        if (b < 1048576) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / 1048576.0);
    }
}
