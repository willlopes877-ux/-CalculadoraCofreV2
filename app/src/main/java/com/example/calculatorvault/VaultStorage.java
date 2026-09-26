package com.example.calculatorvault;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VaultStorage {
    private final Context context;
    private final SharedPreferences meta;

    public VaultStorage(Context c) {
        context = c.getApplicationContext();
        meta = context.getSharedPreferences("vault_metadata", Context.MODE_PRIVATE);
    }

    private File dir(VaultType type) {
        File base = new File(context.getNoBackupFilesDir(), "vault");
        File folder = new File(base, type == VaultType.REAL ? "real" : "fake");
        if (!folder.exists() && !folder.mkdirs() && !folder.exists()) {
            throw new IllegalStateException("Não foi possível criar a pasta do cofre.");
        }
        return folder;
    }

    public File importFile(Uri uri, VaultType type) throws Exception {
        String mime = context.getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";
        if (!mime.startsWith("image/") && !mime.startsWith("video/")) {
            throw new IllegalArgumentException("Selecione somente fotos ou vídeos.");
        }

        String name = queryName(uri);
        if (name == null || name.trim().isEmpty()) {
            name = "arquivo_" + System.currentTimeMillis();
        }

        File out = new File(dir(type), UUID.randomUUID() + ".vault");
        String id = out.getName().replace(".vault", "");

        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Não foi possível abrir o arquivo selecionado.");
            VaultCrypto.encrypt(in, out);
        } catch (Exception e) {
            if (out.exists()) out.delete();
            throw e;
        }

        meta.edit()
                .putString(id + ".name", name)
                .putString(id + ".mime", mime)
                .apply();

        return out;
    }

    private String queryName(Uri uri) {
        try (Cursor c = context.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return null;
    }

    public List<VaultItem> getItems(VaultType type) {
        List<VaultItem> result = new ArrayList<>();
        for (File file : getFiles(type)) {
            String id = file.getName().replace(".vault", "");
            result.add(new VaultItem(
                    file,
                    meta.getString(id + ".name", file.getName()),
                    meta.getString(id + ".mime", "application/octet-stream"),
                    file.length(),
                    file.lastModified()
            ));
        }
        return result;
    }

    public List<File> getFiles(VaultType type) {
        File[] files = dir(type).listFiles();
        List<File> result = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".vault")) result.add(file);
            }
        }
        return result;
    }

    public void deleteItem(VaultItem item) {
        if (item == null) return;
        String id = item.getFile().getName().replace(".vault", "");
        item.getFile().delete();
        meta.edit().remove(id + ".name").remove(id + ".mime").apply();
    }

    public File createTemporaryDecryptedFile(File encrypted, String mime) throws Exception {
        File cache = new File(context.getCacheDir(), "vault_temp");
        if (!cache.exists() && !cache.mkdirs() && !cache.exists()) {
            throw new IOException("Não foi possível criar a área temporária.");
        }

        String extension = ".bin";
        if (mime != null && mime.startsWith("video/")) extension = ".mp4";
        else if (mime != null && mime.startsWith("image/")) extension = ".jpg";

        File output = new File(cache, UUID.randomUUID() + extension);
        try {
            VaultCrypto.decrypt(encrypted, output);
            return output;
        } catch (Exception e) {
            if (output.exists()) output.delete();
            throw e;
        }
    }

    public void clearTemporaryFiles() {
        File cache = new File(context.getCacheDir(), "vault_temp");
        File[] files = cache.listFiles();
        if (files != null) for (File file : files) if (file.isFile()) file.delete();
    }

    public long getStorageUsed(VaultType type) {
        long total = 0;
        for (File file : getFiles(type)) total += file.length();
        return total;
    }
}
