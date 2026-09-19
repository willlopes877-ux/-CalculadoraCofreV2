package com.example.calculatorvault;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VaultStorage {

    private final Context context;

    public VaultStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    private File getVaultDirectory(VaultType type) {

        File base =
                new File(
                        context.getNoBackupFilesDir(),
                        "vault"
                );

        File folder;

        if (type == VaultType.REAL) {
            folder = new File(base, "real");
        } else {
            folder = new File(base, "fake");
        }

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    public File importFile(
            Uri uri,
            VaultType type
    ) throws Exception {

        File folder =
                getVaultDirectory(type);

        String id =
                UUID.randomUUID().toString();

        File destination =
                new File(
                        folder,
                        id + ".vault"
                );

        try (
                InputStream input =
                        context
                                .getContentResolver()
                                .openInputStream(uri)
        ) {

            if (input == null) {
                throw new Exception(
                        "Não foi possível abrir o arquivo."
                );
            }

            if (type == VaultType.REAL) {

                VaultCrypto.encrypt(
                        input,
                        destination
                );

            } else {

                copy(
                        input,
                        destination
                );
            }
        }

        return destination;
    }

    public List<File> getFiles(
            VaultType type
    ) {

        File folder =
                getVaultDirectory(type);

        File[] files =
                folder.listFiles();

        List<File> result =
                new ArrayList<>();

        if (files != null) {

            for (File file : files) {

                if (file.isFile()
                        && file.getName()
                        .endsWith(".vault")) {

                    result.add(file);
                }
            }
        }

        return result;
    }

    public void deleteFile(
            File file
    ) {

        if (file != null
                && file.exists()) {

            file.delete();
        }
    }

    public File createTemporaryDecryptedFile(
            File encryptedFile
    ) throws Exception {

        File cacheDir =
                new File(
                        context.getCacheDir(),
                        "vault_temp"
                );

        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File output =
                new File(
                        cacheDir,
                        UUID.randomUUID()
                                .toString()
                                + ".tmp"
                );

        VaultCrypto.decrypt(
                encryptedFile,
                output
        );

        return output;
    }

    public void clearTemporaryFiles() {

        File cacheDir =
                new File(
                        context.getCacheDir(),
                        "vault_temp"
                );

        if (!cacheDir.exists()) {
            return;
        }

        File[] files =
                cacheDir.listFiles();

        if (files != null) {

            for (File file : files) {

                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }

    public long getStorageUsed(
            VaultType type
    ) {

        long total = 0;

        for (File file : getFiles(type)) {
            total += file.length();
        }

        return total;
    }

    private void copy(
            InputStream input,
            File destination
    ) throws Exception {

        try (
                FileOutputStream output =
                        new FileOutputStream(
                                destination
                        )
        ) {

            byte[] buffer =
                    new byte[8192];

            int count;

            while (
                    (count = input.read(buffer))
                            != -1
            ) {

                output.write(
                        buffer,
                        0,
                        count
                );
            }
        }
    }
}