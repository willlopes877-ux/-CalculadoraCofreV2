package com.example.calculatorvault;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PinManager {

    private static final String PREFS = "vault_auth";

    private static final String REAL_HASH = "real_pin_hash";
    private static final String REAL_SALT = "real_pin_salt";

    private static final String FAKE_HASH = "fake_pin_hash";
    private static final String FAKE_SALT = "fake_pin_salt";

    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;

    private final SharedPreferences prefs;

    public PinManager(Context context) {
        prefs = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
    }

    public boolean isRealPinConfigured() {
        return prefs.contains(REAL_HASH)
                && prefs.contains(REAL_SALT);
    }

    public boolean isFakePinConfigured() {
        return prefs.contains(FAKE_HASH)
                && prefs.contains(FAKE_SALT);
    }

    public void setRealPin(String pin) throws Exception {
        savePin(
                pin,
                REAL_HASH,
                REAL_SALT
        );
    }

    public void setFakePin(String pin) throws Exception {
        savePin(
                pin,
                FAKE_HASH,
                FAKE_SALT
        );
    }

    public boolean verifyRealPin(String pin) {
        return verifyPin(
                pin,
                REAL_HASH,
                REAL_SALT
        );
    }

    public boolean verifyFakePin(String pin) {
        return verifyPin(
                pin,
                FAKE_HASH,
                FAKE_SALT
        );
    }

    private void savePin(
            String pin,
            String hashKey,
            String saltKey
    ) throws Exception {

        if (pin == null || pin.length() < 4) {
            throw new IllegalArgumentException(
                    "O PIN deve ter pelo menos 4 números."
            );
        }

        byte[] salt = new byte[16];

        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        byte[] hash = deriveKey(
                pin.toCharArray(),
                salt
        );

        prefs.edit()
                .putString(
                        hashKey,
                        Base64.encodeToString(
                                hash,
                                Base64.NO_WRAP
                        )
                )
                .putString(
                        saltKey,
                        Base64.encodeToString(
                                salt,
                                Base64.NO_WRAP
                        )
                )
                .apply();
    }

    private boolean verifyPin(
            String pin,
            String hashKey,
            String saltKey
    ) {

        try {

            String savedHash =
                    prefs.getString(
                            hashKey,
                            null
                    );

            String savedSalt =
                    prefs.getString(
                            saltKey,
                            null
                    );

            if (savedHash == null ||
                    savedSalt == null) {

                return false;
            }

            byte[] salt =
                    Base64.decode(
                            savedSalt,
                            Base64.NO_WRAP
                    );

            byte[] calculated =
                    deriveKey(
                            pin.toCharArray(),
                            salt
                    );

            byte[] expected =
                    Base64.decode(
                            savedHash,
                            Base64.NO_WRAP
                    );

            if (calculated.length !=
                    expected.length) {

                return false;
            }

            int result = 0;

            for (int i = 0;
                 i < calculated.length;
                 i++) {

                result |=
                        calculated[i]
                        ^ expected[i];
            }

            return result == 0;

        } catch (Exception e) {

            return false;
        }
    }

    private byte[] deriveKey(
            char[] pin,
            byte[] salt
    ) throws Exception {

        PBEKeySpec spec =
                new PBEKeySpec(
                        pin,
                        salt,
                        ITERATIONS,
                        KEY_LENGTH
                );

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance(
                        "PBKDF2WithHmacSHA256"
                );

        return factory
                .generateSecret(spec)
                .getEncoded();
    }
}