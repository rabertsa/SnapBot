package com.jatmagasoft.snapbot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class FloatingButtonService extends Service {

    private static final String TAG = "FloatingButton";
    private WindowManager windowManager;
    private View floatingView;
    private LinearLayout menuLayout;
    private Button btnToggle, btnLog;
    private TextView txtCount;
    private boolean isMenuOpen = false;
    private boolean isBotRunning = false;
    private Handler handler = new Handler();

    private int snapCount = 0;
    private String lastError = "";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannel();
            startForeground(2, createNotification());

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button, null);
            menuLayout = floatingView.findViewById(R.id.menuLayout);
            btnToggle = floatingView.findViewById(R.id.btnToggle);
            btnLog = floatingView.findViewById(R.id.btnLog);
            txtCount = floatingView.findViewById(R.id.txtCount);

            menuLayout.setVisibility(View.GONE);

            floatingView.findViewById(R.id.btnMain).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleMenu();
                }
            });

            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleBot();
                }
            });

            btnLog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLogDialog();
                }
            });

            int layoutFlag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 100;
            params.y = 300;

            windowManager.addView(floatingView, params);

            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, params);
                            return true;
                    }
                    return false;
                }
            });

            // Log listener
            SnapBotService.setLogListener(new SnapBotService.LogListener() {
                @Override
                public void onSnapSent(int count) {
                    snapCount = count;
                    updateUI();
                }

                @Override
                public void onError(String error) {
                    lastError = error;
                    updateUI();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
            Toast.makeText(this, "Servis başlatılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void toggleMenu() {
        isMenuOpen = !isMenuOpen;
        menuLayout.setVisibility(isMenuOpen ? View.VISIBLE : View.GONE);
    }

    private void toggleBot() {
        if (isBotRunning) {
            Intent intent = new Intent(this, SnapBotService.class);
            stopService(intent);
            isBotRunning = false;
            btnToggle.setText("▶ Başlat");
            btnToggle.setBackgroundColor(0xFF4CAF50);
            Toast.makeText(this, "Bot durduruldu", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, SnapBotService.class);
            intent.putExtra("action", "start");
            startService(intent);
            isBotRunning = true;
            btnToggle.setText("⏹ Durdur");
            btnToggle.setBackgroundColor(0xFFF44336);
            Toast.makeText(this, "Bot başlatıldı! Snapchat'i aç.", Toast.LENGTH_LONG).show();
        }
        updateUI();
    }

    private void showLogDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("📊 SnapBot Log");
        StringBuilder msg = new StringBuilder();
        msg.append("📸 Gönderilen snap: ").append(snapCount).append("\n");
        if (!lastError.isEmpty()) {
            msg.append("❌ Son hata: ").append(lastError);
        } else {
            msg.append("✅ Son durum: Başarılı");
        }
        builder.setMessage(msg.toString());
        builder.setPositiveButton("Kapat", null);
        builder.setNeutralButton("Sıfırla", (dialog, which) -> {
            snapCount = 0;
            lastError = "";
            updateUI();
            Toast.makeText(this, "Log sıfırlandı", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void updateUI() {
        handler.post(() -> {
            txtCount.setText("📸 " + snapCount);
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "floating_channel",
                    "SnapBot Kayan Buton",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, "floating_channel")
                .setContentTitle("SnapBot")
                .setContentText("Kayan buton aktif")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
        SnapBotService.setLogListener(null);
    }
}