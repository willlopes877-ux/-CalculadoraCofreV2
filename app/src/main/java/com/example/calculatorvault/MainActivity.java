package com.example.calculatorvault;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private String input = "";
    private TextView display;

    private PinManager pinManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pinManager = new PinManager(this);

        showCalculator();

        if (!pinManager.isRealPinConfigured()
                || !pinManager.isFakePinConfigured()) {

            showFirstSetup();
        }
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

    private TextView createText(
            String text,
            float size
    ) {

        TextView t = new TextView(this);

        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);

        return t;
    }

    private void showCalculator() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(16),
                dp(28),
                dp(16),
                dp(16)
        );

        root.setBackgroundColor(
                Color.rgb(16, 16, 20)
        );

        display =
                createText(
                        "0",
                        42
                );

        display.setGravity(
                Gravity.RIGHT |
                Gravity.CENTER_VERTICAL
        );

        root.addView(
                display,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        GridLayout grid =
                new GridLayout(this);

        grid.setColumnCount(4);

        String[] keys = {
                "AC", "%", "÷", "⌫",
                "7", "8", "9", "×",
                "4", "5", "6", "−",
                "1", "2", "3", "+",
                "0", ".", "=", "🔒"
        };

        for (String key : keys) {

            Button button =
                    new Button(this);

            button.setText(key);
            button.setTextSize(18);

            button.setOnClickListener(
                    v -> press(key)
            );

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = 0;
            params.height = dp(64);

            params.columnSpec =
                    GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                    );

            params.setMargins(
                    dp(3),
                    dp(3),
                    dp(3),
                    dp(3)
            );

            grid.addView(
                    button,
                    params
            );
        }

        root.addView(
                grid,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        3
                )
        );

        setContentView(root);
    }

    private void press(String key) {

        if (key.equals("🔒")) {

            showPin();

            return;
        }

        if (key.equals("AC")) {

            input = "";
            display.setText("0");

        } else if (key.equals("⌫")) {

            if (!input.isEmpty()) {

                input =
                        input.substring(
                                0,
                                input.length() - 1
                        );
            }

            display.setText(
                    input.isEmpty()
                            ? "0"
                            : input
            );

        } else if (key.equals("=")) {

            display.setText(
                    input.isEmpty()
                            ? "0"
                            : input
            );

        } else {

            input += key;

            display.setText(input);
        }
    }

    private void showFirstSetup() {

        final EditText realPin =
                createPinInput(
                        "Crie o PIN principal"
                );

        final EditText fakePin =
                createPinInput(
                        "Crie o PIN falso"
                );

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(20),
                dp(10),
                dp(20),
                0
        );

        box.addView(realPin);

        box.addView(fakePin);

        new AlertDialog.Builder(this)
                .setTitle(
                        "Configurar cofre"
                )
                .setMessage(
                        "O PIN principal abre o cofre real.\n\n"
                        + "O PIN falso abre o cofre separado."
                )
                .setView(box)
                .setCancelable(false)
                .setPositiveButton(
                        "Salvar",
                        (dialog, which) -> {

                            String real =
                                    realPin
                                            .getText()
                                            .toString();

                            String fake =
                                    fakePin
                                            .getText()
                                            .toString();

                            if (real.length() < 4
                                    || fake.length() < 4) {

                                Toast.makeText(
                                        this,
                                        "Os PINs precisam ter pelo menos 4 números.",
                                        Toast.LENGTH_LONG
                                ).show();

                                showFirstSetup();

                                return;
                            }

                            if (real.equals(fake)) {

                                Toast.makeText(
                                        this,
                                        "O PIN real e o PIN falso precisam ser diferentes.",
                                        Toast.LENGTH_LONG
                                ).show();

                                showFirstSetup();

                                return;
                            }

                            try {

                                pinManager.setRealPin(
                                        real
                                );

                                pinManager.setFakePin(
                                        fake
                                );

                                Toast.makeText(
                                        this,
                                        "Cofres configurados.",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "Erro ao configurar os PINs.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                )
                .show();
    }

    private EditText createPinInput(
            String hint
    ) {

        EditText input =
                new EditText(this);

        input.setHint(hint);

        input.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType
                        .TYPE_NUMBER_VARIATION_PASSWORD
        );

        return input;
    }

    private void showPin() {

        EditText pin =
                createPinInput("PIN");

        new AlertDialog.Builder(this)
                .setTitle(
                        "Digite o resultado"
                )
                .setView(pin)
                .setPositiveButton(
                        "OK",
                        (dialog, which) -> {

                            String value =
                                    pin.getText()
                                            .toString();

                            if (pinManager
                                    .verifyRealPin(value)) {

                                openVault(
                                        VaultType.REAL
                                );

                            } else if (
                                    pinManager
                                            .verifyFakePin(
                                                    value
                                            )) {

                                openVault(
                                        VaultType.FAKE
                                );

                            } else {

                                Toast.makeText(
                                        this,
                                        "Resultado inválido",
                                        Toast.LENGTH_SHORT
                                ).show();

                                showDecoy();
                            }
                        }
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .show();
    }

    private void openVault(
            VaultType type
    ) {

        Intent intent =
                new Intent(
                        this,
                        VaultActivity.class
                );

        intent.putExtra(
                "VAULT_TYPE",
                type == VaultType.REAL
                        ? "REAL"
                        : "FAKE"
        );

        startActivity(intent);
    }

    private void showDecoy() {

        input = "";

        if (display != null) {
            display.setText("0");
        }

        Toast.makeText(
                this,
                "Calculadora pronta",
                Toast.LENGTH_SHORT
        ).show();
    }
}