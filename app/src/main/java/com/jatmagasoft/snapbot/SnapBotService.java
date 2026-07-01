package com.jatmagasoft.snapbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.Random;

public class SnapBotService extends AccessibilityService {

    private Handler handler = new Handler();
    private boolean isRunning = false;
    private Random random = new Random();
    private static final String CHANNEL_ID = "SnapBotChannel";
    private NotificationManager notificationManager;
    private boolean isProcessing = false;
    private String currentPackage = "";

    // Kalibrasyon koordinatları
    private int shutterX = -1, shutterY = -1;
    private int sendX = -1, sendY = -1;
    private int shortcutX = -1, shortcutY = -1;
    private int finalSendX = -1, finalSendY = -1;

    private SharedPreferences prefs;
    private static final String PREF_NAME = "SnapBotPrefs";

    // LOG
    private static int snapCounter = 0;
    private static String lastError = "";
    public static int getSnapCount() { return snapCounter; }
    public static String getLastError() { return lastError; }
    public static void resetLog() { snapCounter = 0; lastError = ""; }

    private static LogListener logListener;
    public interface LogListener {
        void onSnapSent(int count);
        void onError(String error);
    }
    public static void setLogListener(LogListener listener) { logListener = listener; }

    // 🛑 ACİL DURDURMA (Servisi de durdurur)
    public void forceStop() {
        isRunning = false;
        isProcessing = false;
        handler.removeCallbacksAndMessages(null);
        sendNotification("SnapBot", "🛑 Bot durduruldu!");
        Toast.makeText(this, "🛑 Bot durduruldu!", Toast.LENGTH_SHORT).show();
        stopForeground(true);
        stopSelf(); // Servisi sonlandır
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isRunning) return;

        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        currentPackage = pkg;

        if (!pkg.equals("com.snapchat.android")) {
            if (isProcessing) {
                isProcessing = false;
                sendNotification("SnapBot", "⏹ Snapchat kapatıldı.");
            }
            return;
        }

        int eventType = event.getEventType();
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        if (isProcessing) return;

        // Kalibrasyon kontrolü
        if (shutterX == -1 || sendX == -1) {
            sendNotification("SnapBot", "⚠️ Kalibrasyon yapılmamış!");
            return;
        }

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            if (isRunning && !isProcessing && currentPackage.equals("com.snapchat.android")) {
                performSnapAction();
            }
        }, 1500);
    }

    private void performSnapAction() {
        if (isProcessing) return;
        if (!isRunning) {
            sendNotification("SnapBot", "⏹ Bot durduruldu.");
            return;
        }
        if (!currentPackage.equals("com.snapchat.android")) {
            sendNotification("SnapBot", "⏹ Snapchat kapalı.");
            return;
        }
        if (shutterX == -1 || sendX == -1) {
            sendNotification("SnapBot", "⚠️ Kalibrasyon eksik!");
            return;
        }

        isProcessing = true;

        try {
            // 1. DEKLANŞÖR
            sendNotification("SnapBot", "📸 Deklanşör tıklanıyor...");
            performClick(shutterX, shutterY);
            sleep(1000);

            if (!isRunning) { isProcessing = false; return; }

            // 2. GÖNDER
            sendNotification("SnapBot", "📤 Gönder tıklanıyor...");
            performClick(sendX, sendY);
            sleep(1000);

            if (!isRunning) { isProcessing = false; return; }

            // 3. KISAYOL
            if (shortcutX != -1 && shortcutY != -1) {
                sendNotification("SnapBot", "👥 Kısayol tıklanıyor...");
                performClick(shortcutX, shortcutY);
                sleep(1000);
            }

            if (!isRunning) { isProcessing = false; return; }

            // 4. SON GÖNDER
            if (finalSendX != -1 && finalSendY != -1) {
                sendNotification("SnapBot", "✅ Son gönder tıklanıyor...");
                performClick(finalSendX, finalSendY);
                sleep(500);
            } else {
                performClick(sendX, sendY);
            }

            // BAŞARILI
            if (isRunning) {
                snapCounter++;
                lastError = "";
                if (logListener != null) logListener.onSnapSent(snapCounter);
                sendNotification("SnapBot", "✅ Snap gönderildi! Toplam: " + snapCounter);
                Toast.makeText(this, "✅ Snap gönderildi! (" + snapCounter + ")", Toast.LENGTH_SHORT).show();
            }

            isProcessing = false;
            scheduleNext();

        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : "Bilinmeyen hata";
            lastError = errMsg;
            if (logListener != null) logListener.onError(errMsg);
            sendNotification("SnapBot", "❌ Hata: " + errMsg);
            e.printStackTrace();
            isProcessing = false;
            scheduleNext();
        }
    }

    private void scheduleNext() {
        // Sadece bot çalışıyorsa ve Snapchat açıksa devam et
        if (isRunning && currentPackage.equals("com.snapchat.android")) {
            handler.postDelayed(() -> {
                if (isRunning && currentPackage.equals("com.snapchat.android")) {
                    performSnapAction();
                }
            }, random.nextInt(4000) + 3000);
        }
    }

    private void performClick(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            android.accessibilityservice.GestureDescription.Builder builder =
                    new android.accessibilityservice.GestureDescription.Builder();
            builder.addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(clickPath, 0, 1));
            dispatchGesture(builder.build(), null, null);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void sendNotification(String title, String text) {
        if (notificationManager == null) return;
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(2, notification);
    }

    // ========== KALİBRASYON METODLARI ==========
    public void calibrateShutter(int x, int y) {
        shutterX = x; shutterY = y;
        prefs.edit().putInt("shutterX", x).putInt("shutterY", y).apply();
        Toast.makeText(this, "✅ Deklanşör kalibre edildi: (" + x + "," + y + ")", Toast.LENGTH_SHORT).show();
    }
    public void calibrateSend(int x, int y) {
        sendX = x; sendY = y;
        prefs.edit().putInt("sendX", x).putInt("sendY", y).apply();
        Toast.makeText(this, "✅ Gönder kalibre edildi: (" + x + "," + y + ")", Toast.LENGTH_SHORT).show();
    }
    public void calibrateShortcut(int x, int y) {
        shortcutX = x; shortcutY = y;
        prefs.edit().putInt("shortcutX", x).putInt("shortcutY", y).apply();
        Toast.makeText(this, "✅ Kısayol kalibre edildi: (" + x + "," + y + ")", Toast.LENGTH_SHORT).show();
    }
    public void calibrateFinalSend(int x, int y) {
        finalSendX = x; finalSendY = y;
        prefs.edit().putInt("finalSendX", x).putInt("finalSendY", y).apply();
        Toast.makeText(this, "✅ Son gönder kalibre edildi: (" + x + "," + y + ")", Toast.LENGTH_SHORT).show();
    }

    // ========== YAŞAM DÖNGÜSÜ ==========
    @Override
    public void onInterrupt() {}

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        try {
            prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            shutterX = prefs.getInt("shutterX", -1);
            shutterY = prefs.getInt("shutterY", -1);
            sendX = prefs.getInt("sendX", -1);
            sendY = prefs.getInt("sendY", -1);
            shortcutX = prefs.getInt("shortcutX", -1);
            shortcutY = prefs.getInt("shortcutY", -1);
            finalSendX = prefs.getInt("finalSendX", -1);
            finalSendY = prefs.getInt("finalSendY", -1);

            startForegroundService();
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                              AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.notificationTimeout = 100;
            setServiceInfo(info);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (shutterX == -1 || sendX == -1) {
                sendNotification("SnapBot", "⚠️ Kalibrasyon gerekli!");
            } else {
                sendNotification("SnapBot", "✅ Kalibrasyon mevcut, bot hazır.");
            }
            Toast.makeText(this, "SnapBot servisi bağlandı", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Servis hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void startForegroundService() {
        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SnapBot")
                .setContentText("Servis çalışıyor...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SnapBot Servisi",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "start".equals(intent.getStringExtra("action"))) {
            isRunning = true;
            try {
                startForegroundService();
            } catch (Exception e) {
                e.printStackTrace();
            }
            sendNotification("SnapBot", "🚀 Bot başlatıldı! Snapchat'i aç.");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        forceStop(); // Servis yok edilirken acil durdur
        super.onDestroy();
    }
}