package com.example.calculatorvault;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class VaultCrypto {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "CalculatorVaultKey";

    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;

    private static SecretKey getOrCreateKey() throws Exception {

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry)
                    keyStore.getEntry(KEY_ALIAS, null))
                    .getSecretKey();
        }

        KeyGenerator generator =
                KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        KEYSTORE
                );

        generator.init(
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT
                                | KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(
                                KeyProperties.BLOCK_MODE_GCM
                        )
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .build()
        );

        return generator.generateKey();
    }

    public static void encrypt(
            InputStream input,
            File outputFile
    ) throws Exception {

        SecretKey key = getOrCreateKey();

        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
        );

        cipher.init(
                Cipher.ENCRYPT_MODE,
                key,
                new GCMParameterSpec(TAG_SIZE, iv)
        );

        try (
                FileOutputStream output =
                        new FileOutputStream(outputFile)
        ) {

            // Guarda o IV no começo do arquivo.
            output.write(iv);

            processStream(
                    input,
                    output,
                    cipher
            );
        }
    }

    public static void decrypt(
            File encryptedFile,
            File outputFile
    ) throws Exception {

        SecretKey key = getOrCreateKey();

        try (
                FileInputStream input =
                        new FileInputStream(encryptedFile);
                FileOutputStream output =
                        new FileOutputStream(outputFile)
        ) {

            byte[] iv = new byte[IV_SIZE];

            int read = input.read(iv);

            if (read != IV_SIZE) {
                throw new Exception(
                        "Arquivo protegido inválido."
                );
            }

            Cipher cipher = Cipher.getInstance(
                    "AES/GCM/NoPadding"
            );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_SIZE, iv)
            );

            processStream(
                    input,
                    output,
                    cipher
            );
        }
    }

    private static void processStream(
            InputStream input,
            OutputStream output,
            Cipher cipher
    ) throws Exception {

        byte[] buffer = new byte[8192];

        int count;

        while ((count = input.read(buffer)) != -1) {

            byte[] encrypted =
                    cipher.update(
                            buffer,
                            0,
                            count
                    );

            if (encrypted != null) {
                output.write(encrypted);
            }
        }

        byte[] finalBytes =
                cipher.doFinal();

        if (finalBytes != null) {
            output.write(finalBytes);
        }
    }
}