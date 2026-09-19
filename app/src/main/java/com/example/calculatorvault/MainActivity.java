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

    private final String secret = "2580";
    private String input = "";
    private TextView display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showCalculator();
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private TextView createText(String text, float size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        return t;
    }

    private void showCalculator() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(28), dp(16), dp(16));
        root.setBackgroundColor(Color.rgb(16, 16, 20));

        display = createText("0", 42);
        display.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        root.addView(
                display,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);

        String[] keys = {
                "AC", "%", "÷", "⌫",
                "7", "8", "9", "×",
                "4", "5", "6", "−",
                "1", "2", "3", "+",
                "0", ".", "=", "🔒"
        };

        for (String key : keys) {

            Button button = new Button(this);
            button.setText(key);
            button.setTextSize(18);

            button.setOnClickListener(v -> press(key));

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

            grid.addView(button, params);
        }

        root.addView(
                grid,
                new LinearLayout.LayoutParams(-1, 0, 3)
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
                input = input.substring(
                        0,
                        input.length() - 1
                );
            }

            display.setText(
                    input.isEmpty() ? "0" : input
            );

        } else if (key.equals("=")) {

            display.setText(
                    input.isEmpty() ? "0" : input
            );

        } else {

            input += key;
            display.setText(input);
        }
    }

    private void showPin() {

        EditText pin = new EditText(this);

        pin.setHint("PIN");

        pin.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        new AlertDialog.Builder(this)
                .setTitle("Digite o resultado")
                .setView(pin)

                .setPositiveButton(
                        "OK",
                        (dialog, which) -> {

                            if (secret.equals(
                                    pin.getText().toString())) {

                                showVault();

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

    private void showDecoy() {

        input = "";
        display.setText("0");

        Toast.makeText(
                this,
                "Calculadora pronta",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showVault() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(24),
                dp(30),
                dp(24),
                dp(24)
        );

        root.setBackgroundColor(
                Color.rgb(16, 16, 20)
        );

        TextView title =
                createText(
                        "🔒 Cofre privado",
                        28
                );

        root.addView(title);

        TextView info =
                createText(
                        "\nNenhum arquivo importado." +
                        "\n\nAdicione fotos ou vídeos " +
                        "usando o botão abaixo.",
                        18
                );

        root.addView(
                info,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        Button add =
                new Button(this);

        add.setText(
                "Adicionar fotos/vídeos"
        );

        add.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                    );

            intent.setType("*/*");

            intent.putExtra(
                    Intent.EXTRA_ALLOW_MULTIPLE,
                    true
            );

            intent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            startActivityForResult(
                    intent,
                    100
            );
        });

        root.addView(add);

        Button lock =
                new Button(this);

        lock.setText("Bloquear");

        lock.setOnClickListener(
                v -> showCalculator()
        );

        root.addView(lock);

        setContentView(root);
    }
}