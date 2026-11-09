package com.exam.smkn1maltng;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class AlertSoundService extends Service {

    private static final String TAG = "AlertSoundService";

    public static final String ACTION_ALARM_ON  = "com.exam.smkn1maltng.ALARM_ON";
    public static final String ACTION_ALARM_OFF = "com.exam.smkn1maltng.ALARM_OFF";

    private static final String CHANNEL_ID = "exam_penalty_channel_v2";
    private static final int    NOTIF_ID   = 1001;

    // Interval polling volume saat alarm aktif (ms).
    // 500ms cukup responsif tanpa membebani CPU.
    private static final long VOLUME_POLL_INTERVAL_MS = 500L;

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private ViolationManager violationManager;
    private boolean isPlaying = false;

    private Handler pollHandler;

    /** Polling penalti: cek apakah timer habis. */
    private Runnable penaltyPollRunnable;

    /** Polling volume: pastikan volume MUSIC selalu max selama alarm. */
    private Runnable volumePollRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        violationManager = new ViolationManager(this);

        createNotificationChannel();

        pollHandler = new Handler(Looper.getMainLooper());

        // --- Polling penalti: hentikan alarm saat timer habis ---
        penaltyPollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && !violationManager.isInPenalty()) {
                    Log.d(TAG, "penalty poll: expired, stopping alarm");
                    stopAlarmInternal();
                }
                pollHandler.postDelayed(this, 1000);
            }
        };
        pollHandler.post(penaltyPollRunnable);

        // --- Polling volume: paksa volume MUSIC selalu max saat alarm ---
        volumePollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    enforceMaxVolume();
                    pollHandler.postDelayed(this, VOLUME_POLL_INTERVAL_MS);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;
        Log.d(TAG, "onStartCommand action=" + action);

        ensureForeground();

        if (ACTION_ALARM_ON.equals(action)) {
            startAlarmInternal();
        } else if (ACTION_ALARM_OFF.equals(action)) {
            stopAlarmInternal();
        } else {
            if (violationManager.isInPenalty()) {
                startAlarmInternal();
            } else if (!isPlaying) {
                releaseAll();
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (pollHandler != null) {
            if (penaltyPollRunnable != null) {
                pollHandler.removeCallbacks(penaltyPollRunnable);
            }
            if (volumePollRunnable != null) {
                pollHandler.removeCallbacks(volumePollRunnable);
            }
        }
        releaseAll();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // =================================================================
    // ALARM STATE
    // =================================================================

    private void startAlarmInternal() {
        if (isPlaying) {
            // Sudah main: pastikan volume full & polling tetap jalan.
            enforceMaxVolume();
            startVolumePollIfNeeded();
            return;
        }

        Log.d(TAG, "startAlarmInternal: starting alarm");
        isPlaying = true;
        ensureForeground();
        requestAudioFocus();
        enforceMaxVolume();
        startVolumePollIfNeeded();
        startMediaPlayer();
    }

    private void stopAlarmInternal() {
        if (!isPlaying) return;
        Log.d(TAG, "stopAlarmInternal");
        isPlaying = false;
        stopVolumePoll();
        stopMediaPlayer();
        abandonAudioFocus();
        ensureForeground();
    }

    private void releaseAll() {
        isPlaying = false;
        stopVolumePoll();
        stopMediaPlayer();
        abandonAudioFocus();
    }

    // =================================================================
    // VOLUME ENFORCEMENT (FITUR #2)
    // =================================================================

    private void startVolumePollIfNeeded() {
        if (volumePollRunnable == null) return;
        pollHandler.removeCallbacks(volumePollRunnable);
        pollHandler.post(volumePollRunnable);
    }

    private void stopVolumePoll() {
        if (volumePollRunnable != null) {
            pollHandler.removeCallbacks(volumePollRunnable);
        }
    }

    /**
     * Selama alarm aktif: setiap kali volume user < max, paksa balik ke max
     * tanpa memunculkan UI volume sistem.
     */
    private void enforceMaxVolume() {
        if (audioManager == null) return;
        try {
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (current < max) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0);
                Log.d(TAG, "enforceMaxVolume: " + current + " -> " + max);
            }
        } catch (Exception e) {
            Log.e(TAG, "enforceMaxVolume error", e);
        }
    }

    // =================================================================
    // MEDIA PLAYER
    // =================================================================

    private void startMediaPlayer() {
        stopMediaPlayer();
        try {
            AssetFileDescriptor afd = getAssets().openFd("alert_sound.mp3");
            mediaPlayer = new MediaPlayer();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                mediaPlayer.setAudioAttributes(attrs);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            mediaPlayer.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength());
            afd.close();

            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (isPlaying) {
                    mp.start();
                    Log.d(TAG, "MediaPlayer started");
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error what=" + what + " extra=" + extra);
                return false;
            });
        } catch (IOException e) {
            Log.e(TAG, "startMediaPlayer IOException", e);
        } catch (Exception e) {
            Log.e(TAG, "startMediaPlayer Exception", e);
        }
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) { }
            mediaPlayer = null;
        }
    }

    // =================================================================
    // AUDIO FOCUS
    // =================================================================

    private void requestAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(attrs)
                        .setWillPauseWhenDucked(false)
                        .setOnAudioFocusChangeListener(change -> {
                            Log.d(TAG, "AudioFocus change=" + change);
                            if (change == AudioManager.AUDIOFOCUS_GAIN
                                    && mediaPlayer != null && isPlaying
                                    && !mediaPlayer.isPlaying()) {
                                mediaPlayer.start();
                            }
                        })
                        .build();
                audioManager.requestAudioFocus(audioFocusRequest);
            } else {
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
            }
        } catch (Exception e) {
            Log.e(TAG, "requestAudioFocus error", e);
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(null);
            }
        } catch (Exception ignored) { }
    }

    // =================================================================
    // NOTIFICATION
    // =================================================================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Exam Penalty Alert",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Notifikasi alarm pelanggaran ujian");
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
    }

    private void ensureForeground() {
        String title = isPlaying ? "PELANGGARAN TERDETEKSI" : "Ujian Berlangsung";
        String text  = isPlaying
                ? "Suara alarm aktif sampai waktu penalti habis"
                : "Aplikasi ujian aktif";

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setPriority(isPlaying
                        ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }
}
