package com.example.calculatorvault;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private String input = "";
    private double calcValue = 0;
    private String pendingOperator = "";
    private boolean enteringSecond = false;
    private TextView display;
    private PinManager pinManager;

    private final int BG = Color.rgb(0, 0, 0);
    private final int DARK = Color.rgb(51, 51, 51);
    private final int LIGHT = Color.rgb(165, 165, 165);
    private final int ORANGE = Color.rgb(255, 149, 0);
    private final int WHITE = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pinManager = new PinManager(this);
        showCalculator();

        if (!pinManager.isRealPinConfigured() || !pinManager.isFakePinConfigured()) {
            showFirstSetup();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView createDisplay() {
        TextView t = new TextView(this);
        t.setText("0");
        t.setTextColor(WHITE);
        t.setTextSize(46);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setPadding(dp(8), 0, dp(8), 0);
        return t;
    }

    private GradientDrawable circleBackground(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setShape(GradientDrawable.OVAL);
        return bg;
    }

    private Button createKey(String key) {
        Button b = new Button(this);
        b.setText(key);
        b.setTextSize(key.equals("0") ? 22 : 20);
        b.setTextColor(key.equals("AC") || key.equals("⌫") || key.equals("%")
                ? Color.BLACK : WHITE);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setGravity(Gravity.CENTER);

        int bgColor;
        if (key.equals("ENTRAR") || key.equals("÷") || key.equals("×")
                || key.equals("−") || key.equals("+")) {
            bgColor = ORANGE;
        } else if (key.equals("AC") || key.equals("⌫") || key.equals("%")) {
            bgColor = LIGHT;
        } else {
            bgColor = DARK;
        }
        b.setBackground(circleBackground(bgColor));
        b.setOnClickListener(v -> press(key));
        return b;
    }

    private void showCalculator() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(24), dp(8), dp(8));
        root.setBackgroundColor(BG);

        display = createDisplay();
        root.addView(display, new LinearLayout.LayoutParams(-1, 0, 1.35f));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setRowCount(5);

        String[] keys = {
                "AC", "⌫", "%", "÷",
                "7", "8", "9", "×",
                "4", "5", "6", "−",
                "1", "2", "3", "+",
                "0", ".", "ENTRAR", ""
        };

        for (String key : keys) {
            if (key.isEmpty()) continue;
            Button b = createKey(key);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            if (key.equals("0")) {
                p.columnSpec = GridLayout.spec(0, 2, 1f);
            } else {
                p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            }
            p.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            p.width = 0;
            p.height = 0;
            p.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(b, p);
        }

        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 3.8f));
        setContentView(root);
    }

    private void press(String key) {
        if (key.equals("AC")) {
            clearInput();
            return;
        }

        if (key.equals("⌫")) {
            if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            display.setText(input.isEmpty() ? "0" : input);
            return;
        }

        if (key.equals("ENTRAR")) {
            if (!pendingOperator && input.matches("\\d{4,12}")) {
                unlockFromCalculator();
            } else {
                calculateResult();
            }
            return;
        }

        if (key.equals("+") || key.equals("−") || key.equals("×") || key.equals("÷")) {
            if (input.isEmpty() || input.equals("0")) return;
            try {
                calcValue = Double.parseDouble(input);
                pendingOperator = key;
                enteringSecond = true;
                display.setText("0");
                input = "";
            } catch (Exception ignored) {}
            return;
        }

        if (key.equals(".")) {
            if (!input.contains(".")) {
                input = input.isEmpty() ? "0." : input + ".";
                display.setText(input);
            }
            return;
        }

        if (key.matches("\\d")) {
            if (input.length() < 12) {
                input += key;
                display.setText(input);
            }
        }
    }

    private void calculateResult() {
        if (pendingOperator.isEmpty() || input.isEmpty()) return;
        try {
            double second = Double.parseDouble(input);
            double result;
            switch (pendingOperator) {
                case "+": result = calcValue + second; break;
                case "−": result = calcValue - second; break;
                case "×": result = calcValue * second; break;
                case "÷":
                    if (second == 0) throw new ArithmeticException();
                    result = calcValue / second; break;
                default: return;
            }
            String shown = (result == Math.rint(result)) ? String.valueOf((long) result) : String.valueOf(result);
            display.setText(shown);
            input = shown;
            pendingOperator = "";
            enteringSecond = false;
        } catch (Exception e) {
            display.setText("Erro");
            input = "";
            pendingOperator = "";
        }
    }

    private void unlockFromCalculator() {
        if (input.length() < 4) {
            Toast.makeText(this, "Digite o PIN e toque em =", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pinManager.verifyRealPin(input)) {
            openVault(VaultType.REAL);
            clearInput();
        } else if (pinManager.verifyFakePin(input)) {
            openVault(VaultType.FAKE);
            clearInput();
        } else {
            Toast.makeText(this, "Resultado inválido", Toast.LENGTH_SHORT).show();
            clearInput();
        }
    }

    private void clearInput() {
        input = "";
        calcValue = 0;
        pendingOperator = "";
        enteringSecond = false;
        if (display != null) display.setText("0");
    }

    private void showFirstSetup() {
        final EditText pin1 = createPinInput("PIN do Cofre 1");
        final EditText pin2 = createPinInput("PIN do Cofre 2");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(pin1);
        box.addView(pin2);

        new AlertDialog.Builder(this)
                .setTitle("Configurar seus dois cofres")
                .setMessage("Crie dois PINs diferentes. Na calculadora, digite o PIN e toque em ENTRAR para abrir o cofre correspondente.")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton("Salvar", null)
                .create();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Configurar seus dois cofres")
                .setMessage("Crie dois PINs diferentes. Na calculadora, digite o PIN e toque em = para abrir o cofre correspondente.")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton("Salvar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String real = pin1.getText().toString();
            String fake = pin2.getText().toString();

            if (real.length() < 4 || fake.length() < 4) {
                Toast.makeText(this, "Os dois PINs precisam ter pelo menos 4 números.", Toast.LENGTH_LONG).show();
                return;
            }
            if (real.equals(fake)) {
                Toast.makeText(this, "Os dois PINs precisam ser diferentes.", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                pinManager.setRealPin(real);
                pinManager.setFakePin(fake);
                dialog.dismiss();
                Toast.makeText(this, "Cofres configurados.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao configurar os PINs.", Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private EditText createPinInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return input;
    }

    private void openVault(VaultType type) {
        Intent intent = new Intent(this, VaultActivity.class);
        intent.putExtra("VAULT_TYPE", type == VaultType.REAL ? "REAL" : "FAKE");
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearInput();
    }
}
