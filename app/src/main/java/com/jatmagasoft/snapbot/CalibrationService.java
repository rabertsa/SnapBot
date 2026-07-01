package com.jatmagasoft.snapbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class CalibrationService extends AccessibilityService {

    private Handler handler = new Handler();
    private String calibrationType = "";
    private SharedPreferences prefs;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Kullanıcı ekrana dokunduğunda koordinatları al
        if (event.getEventType() == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) {
            // Dokunma koordinatlarını almak için event'ten alınamıyor, dispatchGesture kullanmamız gerek.
            // Bu yöntem çalışmıyor, onun yerine kullanıcıdan manuel koordinat girmesini isteyelim.
            // Şimdilik pasif.
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("SnapBotPrefs", Context.MODE_PRIVATE);
        Toast.makeText(this, "Kalibrasyon servisi bağlandı. Lütfen hedef butona uzun basıp koordinatları not edin.", Toast.LENGTH_LONG).show();
        // Bildirim gönder
        sendNotification("Kalibrasyon", "Hedef butona uzun bas ve koordinatları not et.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("type")) {
            calibrationType = intent.getStringExtra("type");
        }
        return START_NOT_STICKY;
    }

    private void sendNotification(String title, String text) {
        // Basit bildirim
        android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        android.app.Notification notification = new android.app.Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();
        manager.notify(1, notification);
    }
}