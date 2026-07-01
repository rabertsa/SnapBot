package com.jatmagasoft.snapbot;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Button btnStart, btnStop, btnLog, btnPermission, btnBattery;
    private Button btnCalibrateShutter, btnCalibrateSend, btnCalibrateShortcut, btnCalibrateFinal;
    private TextView txtStatus;
    private boolean isRunning = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SnapBotPrefs", MODE_PRIVATE);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnLog = findViewById(R.id.btnLog);
        btnPermission = findViewById(R.id.btnPermission);
        btnBattery = findViewById(R.id.btnBattery);
        txtStatus = findViewById(R.id.txtStatus);

        btnCalibrateShutter = findViewById(R.id.btnCalibrateShutter);
        btnCalibrateSend = findViewById(R.id.btnCalibrateSend);
        btnCalibrateShortcut = findViewById(R.id.btnCalibrateShortcut);
        btnCalibrateFinal = findViewById(R.id.btnCalibrateFinal);

        // Koordinatları göster
        int shutterX = prefs.getInt("shutterX", -1);
        int shutterY = prefs.getInt("shutterY", -1);
        int sendX = prefs.getInt("sendX", -1);
        int sendY = prefs.getInt("sendY", -1);
        int shortcutX = prefs.getInt("shortcutX", -1);
        int shortcutY = prefs.getInt("shortcutY", -1);
        int finalX = prefs.getInt("finalSendX", -1);
        int finalY = prefs.getInt("finalSendY", -1);
        txtStatus.setText("Kalibrasyon: Deklanşör(" + shutterX + "," + shutterY + ") Gönder(" + sendX + "," + sendY + ")");

        btnPermission.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(MainActivity.this,
                    "Açılan ekrandan 'SnapBot' servisini AÇIN.",
                    Toast.LENGTH_LONG).show();
        });

        btnBattery.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(MainActivity.this,
                    "Pil optimizasyonunu kapatın.",
                    Toast.LENGTH_LONG).show();
        });

        btnStart.setOnClickListener(v -> startBot());
        btnStop.setOnClickListener(v -> stopBot());
        btnLog.setOnClickListener(v -> showLogDialog());

        // Kalibrasyon butonları
        btnCalibrateShutter.setOnClickListener(v -> {
            Toast.makeText(this, "📸 Snapchat'i aç ve deklanşöre uzun bas.", Toast.LENGTH_LONG).show();
            // Koordinatları manuel girmek için dialog açılabilir ama kullanıcı zaten koordinatları biliyor.
            // Şimdilik kullanıcı koordinatları manuel girecek.
            showCoordinateDialog("shutter");
        });

        btnCalibrateSend.setOnClickListener(v -> showCoordinateDialog("send"));
        btnCalibrateShortcut.setOnClickListener(v -> showCoordinateDialog("shortcut"));
        btnCalibrateFinal.setOnClickListener(v -> showCoordinateDialog("final"));

        SnapBotService.setLogListener(new SnapBotService.LogListener() {
            @Override
            public void onSnapSent(int count) {
                runOnUiThread(() -> {
                    txtStatus.setText("✅ Gönderim: " + count);
                    prefs.edit().putInt("snapCount", count).apply();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    txtStatus.setText("❌ Hata: " + error);
                    prefs.edit().putString("lastError", error).apply();
                });
            }
        });
    }

    private void showCoordinateDialog(String type) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Koordinatları Gir");
        final android.widget.EditText inputX = new android.widget.EditText(this);
        final android.widget.EditText inputY = new android.widget.EditText(this);
        inputX.setHint("X koordinatı");
        inputY.setHint("Y koordinatı");
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(inputX);
        layout.addView(inputY);
        builder.setView(layout);
        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            try {
                int x = Integer.parseInt(inputX.getText().toString());
                int y = Integer.parseInt(inputY.getText().toString());
                Intent intent = new Intent(this, SnapBotService.class);
                switch (type) {
                    case "shutter":
                        intent.putExtra("calibrate_shutter_x", x);
                        intent.putExtra("calibrate_shutter_y", y);
                        break;
                    case "send":
                        intent.putExtra("calibrate_send_x", x);
                        intent.putExtra("calibrate_send_y", y);
                        break;
                    case "shortcut":
                        intent.putExtra("calibrate_shortcut_x", x);
                        intent.putExtra("calibrate_shortcut_y", y);
                        break;
                    case "final":
                        intent.putExtra("calibrate_final_x", x);
                        intent.putExtra("calibrate_final_y", y);
                        break;
                }
                startService(intent);
                Toast.makeText(this, "✅ Kalibrasyon kaydedildi!", Toast.LENGTH_SHORT).show();
                // UI'ı güncelle
                SharedPreferences.Editor editor = prefs.edit();
                if (type.equals("shutter")) {
                    editor.putInt("shutterX", x).putInt("shutterY", y);
                } else if (type.equals("send")) {
                    editor.putInt("sendX", x).putInt("sendY", y);
                } else if (type.equals("shortcut")) {
                    editor.putInt("shortcutX", x).putInt("shortcutY", y);
                } else if (type.equals("final")) {
                    editor.putInt("finalSendX", x).putInt("finalSendY", y);
                }
                editor.apply();
                int shutterX = prefs.getInt("shutterX", -1);
                int shutterY = prefs.getInt("shutterY", -1);
                int sendX = prefs.getInt("sendX", -1);
                int sendY = prefs.getInt("sendY", -1);
                txtStatus.setText("Kalibrasyon: Deklanşör(" + shutterX + "," + shutterY + ") Gönder(" + sendX + "," + sendY + ")");
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Geçerli bir sayı girin!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    private void startBot() {
        if (isRunning) {
            Toast.makeText(this, "Bot zaten çalışıyor!", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = true;
        txtStatus.setText("⏳ Bot başlatıldı, Snapchat'i aç...");
        Toast.makeText(this, "Bot başlatıldı! Snapchat'i aç.", Toast.LENGTH_LONG).show();

        Intent serviceIntent = new Intent(MainActivity.this, SnapBotService.class);
        serviceIntent.putExtra("action", "start");
        startService(serviceIntent);
    }

    private void stopBot() {
        if (!isRunning) {
            Toast.makeText(this, "Bot zaten durmuş!", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = false;
        txtStatus.setText("⏹ Durduruldu");
        Toast.makeText(this, "Bot durduruluyor...", Toast.LENGTH_SHORT).show();

        Intent serviceIntent = new Intent(MainActivity.this, SnapBotService.class);
        stopService(serviceIntent);
        SnapBotService.setLogListener(null);
    }

    private void showLogDialog() {
        int count = SnapBotService.getSnapCount();
        if (count == 0) count = prefs.getInt("snapCount", 0);
        String error = SnapBotService.getLastError();
        if (error == null || error.isEmpty()) error = prefs.getString("lastError", "");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("📊 SnapBot Log");
        StringBuilder msg = new StringBuilder();
        msg.append("📸 Gönderilen snap: ").append(count).append("\n");
        if (error != null && !error.isEmpty()) {
            msg.append("❌ Son hata: ").append(error);
        } else {
            msg.append("✅ Son durum: Başarılı");
        }
        builder.setMessage(msg.toString());
        builder.setPositiveButton("Kapat", null);
        builder.setNeutralButton("Sıfırla", (dialog, which) -> {
            SnapBotService.resetLog();
            prefs.edit().clear().apply();
            txtStatus.setText("📊 Log sıfırlandı");
            Toast.makeText(this, "Log sıfırlandı", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}