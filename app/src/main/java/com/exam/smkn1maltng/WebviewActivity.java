package com.exam.smkn1maltng;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.exam.smkn1maltng.databinding.ActivityWebViewBinding;

import java.util.Locale;

public class WebviewActivity extends AppCompatActivity {
    private static final String TAG = "WebviewActivity";

    private ActivityWebViewBinding binding;
    private WebView myWebView;
    private WebSettings wbst;
    private SharedPreferences sharedPreferences;
    private ViolationManager violationManager;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isLongPressBackTriggered = false;
    private static final int LONG_PRESS_DURATION = 2000;

    private Handler penaltyHandler = new Handler(Looper.getMainLooper());
    private Runnable penaltyRunnable;
    private boolean isInPenalty = false;

    private boolean pinned = false;
    private boolean isFinishingApp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        binding.getRoot().setFilterTouchesWhenObscured(true);

        String url = getIntent().getStringExtra("url");
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "URL tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        violationManager = new ViolationManager(this);
        sharedPreferences = getSharedPreferences("setting", 0);

        if (violationManager.isInPenalty()) {
            Log.d(TAG, "onCreate: masih dalam penalti");
            showPenaltyOverlay();
            return;
        }

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) supportActionBar.hide();

        myWebView = binding.myWebView;
        startScreenPinning();

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        LinearLayout bottomNavBar = binding.bottomNavBar;
        ImageView navBack = binding.navBack;
        ImageView navRefresh = binding.navRefresh;
        ImageView navForward = binding.navForward;
        ImageView navExit = binding.navExit;

        navBack.setOnClickListener(v -> {
            if (myWebView.canGoBack()) myWebView.goBack();
            else Toast.makeText(this, "Tidak ada halaman sebelumnya", Toast.LENGTH_SHORT).show();
        });

        navRefresh.setOnClickListener(v -> {
            myWebView.reload();
            Toast.makeText(this, "Refresh", Toast.LENGTH_SHORT).show();
        });

        navForward.setOnClickListener(v -> {
            if (myWebView.canGoForward()) myWebView.goForward();
            else Toast.makeText(this, "Tidak ada halaman selanjutnya", Toast.LENGTH_SHORT).show();
        });

        navExit.setOnClickListener(v -> {
            isFinishingApp = true;
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.dialog_exam_title)
                .setMessage(R.string.dialog_exam_message)
                .setPositiveButton(R.string.dialog_exam_yes, (dialog, which) -> {
                    stopScreenPinning();
                    stopAlertService();
                    finish();
                })
                .setNegativeButton(R.string.dialog_exam_no, (dialog, which) -> {
                    isFinishingApp = false;
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
        });

        bottomNavBar.setVisibility(View.VISIBLE);

        class MyWebViewClient extends WebViewClient {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                return false;
            }

            @Override
            @Deprecated
            public boolean shouldOverrideUrlLoading(WebView webView, String str) {
                return false;
            }

            @Override
            public void onPageFinished(WebView webView, String url) {
                super.onPageFinished(webView, url);
                binding.loadingTextView.setVisibility(View.GONE);
                myWebView.setVisibility(View.VISIBLE);
                updateNavigationButtons();

                String antiCopyScript =
                    "document.body.style.webkitUserSelect='none';" +
                    "document.body.style.userSelect='none';" +
                    "document.addEventListener('copy', function(e){ e.preventDefault(); return false; });" +
                    "document.addEventListener('paste', function(e){ e.preventDefault(); return false; });" +
                    "document.addEventListener('cut', function(e){ e.preventDefault(); return false; });";
                myWebView.evaluateJavascript(antiCopyScript, null);
            }

            @Override
            public void onReceivedError(WebView webView, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(webView, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (request.isForMainFrame()) {
                        Toast.makeText(WebviewActivity.this, R.string.toast_network_unstable, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(WebviewActivity.this, R.string.toast_network_unstable, Toast.LENGTH_SHORT).show();
                }
            }
        }

        wbst = myWebView.getSettings();
        wbst.setJavaScriptEnabled(true);
        wbst.setAllowContentAccess(true);
        wbst.setAllowFileAccess(true);
        wbst.setDomStorageEnabled(true);

        myWebView.addJavascriptInterface(new JSBridge(), "JSBridge");

        if (sharedPreferences.getBoolean("support_zoom", true)) {
            wbst.setSupportZoom(true);
            wbst.setBuiltInZoomControls(true);
            wbst.setDisplayZoomControls(false);
        }

        myWebView.setWebViewClient(new MyWebViewClient());
        myWebView.loadUrl(url);
    }

    private void stopScreenPinning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                stopLockTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateNavigationButtons() {
        ImageView navBack = binding.navBack;
        ImageView navForward = binding.navForward;

        if (myWebView.canGoBack()) {
            navBack.setAlpha(1.0f);
            navBack.setEnabled(true);
        } else {
            navBack.setAlpha(0.3f);
            navBack.setEnabled(false);
        }

        if (myWebView.canGoForward()) {
            navForward.setAlpha(1.0f);
            navForward.setEnabled(true);
        } else {
            navForward.setAlpha(0.3f);
            navForward.setEnabled(false);
        }
    }

    private void startScreenPinning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startLockTask();
        }
    }

    private void startAlertService() {
        Log.d(TAG, "startAlertService");
        Intent i = new Intent(this, AlertSoundService.class);
        i.setAction(AlertSoundService.ACTION_ALARM_ON);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Exception e) {
            Log.e(TAG, "startForegroundService failed, fallback", e);
            try { startService(i); } catch (Exception ignored) { }
        }
    }

    private void stopAlertService() {
        Log.d(TAG, "stopAlertService");
        Intent i = new Intent(this, AlertSoundService.class);
        i.setAction(AlertSoundService.ACTION_ALARM_OFF);
        try { startService(i); } catch (Exception ignored) { }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "onWindowFocusChanged: hasFocus=" + hasFocus);
        if (!hasFocus && !isFinishingApp) {
            recordViolationAndPenalty();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: called");
        if (!isFinishingApp) {
            recordViolationAndPenalty();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: called");
        if (isInPenalty && violationManager != null && violationManager.isInPenalty()) {
            startAlertService();
        }
    }

    private void recordViolationAndPenalty() {
        if (violationManager == null) return;
        if (isInPenalty) {
            Log.d(TAG, "recordViolationAndPenalty: already in penalty, skip");
            return;
        }

        violationManager.recordViolation();
        Log.d(TAG, "recordViolationAndPenalty: count=" + violationManager.getViolationCount());

        if (violationManager.isInPenalty()) {
            runOnUiThread(this::showPenaltyOverlay);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ASSIST ||
            keyCode == KeyEvent.KEYCODE_APP_SWITCH ||
            keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isLongPressBackTriggered) return true;
            handler.postDelayed(() -> isLongPressBackTriggered = false, LONG_PRESS_DURATION);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isLongPressBackTriggered = true;
            showExitDialog();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    private void showExitDialog() {
        isFinishingApp = true;
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.dialog_exam_title)
            .setMessage(R.string.dialog_exam_message)
            .setPositiveButton(R.string.dialog_exam_yes, (dialog, which) -> {
                stopScreenPinning();
                stopAlertService();
                finish();
            })
            .setNegativeButton(R.string.dialog_exam_no, (dialog, which) -> {
                isFinishingApp = false;
                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
    }

    @Override
    public void onBackPressed() {
        if (myWebView != null && myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            isFinishingApp = true;
            stopScreenPinning();
            stopAlertService();
            finish();
        }
    }

    private void showPenaltyOverlay() {
        isInPenalty = true;
        binding.penaltyOverlay.setVisibility(View.VISIBLE);
        binding.tvPenaltyCount.setText(String.format(
                getString(R.string.penalty_count),
                violationManager.getViolationCount()));

        startAlertService();
        startPenaltyTimer();
    }

    private void startPenaltyTimer() {
        if (penaltyRunnable != null) penaltyHandler.removeCallbacks(penaltyRunnable);
        penaltyRunnable = new Runnable() {
            @Override
            public void run() {
                if (!violationManager.isInPenalty()) {
                    hidePenaltyOverlay();
                    return;
                }
                long remainingSeconds = violationManager.getRemainingTimeInSeconds();
                binding.tvPenaltyTimer.setText(String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        remainingSeconds / 60,
                        remainingSeconds % 60));
                penaltyHandler.postDelayed(this, 1000);
            }
        };
        penaltyHandler.post(penaltyRunnable);
    }

    private void hidePenaltyOverlay() {
        isInPenalty = false;
        binding.penaltyOverlay.setVisibility(View.GONE);
        if (penaltyRunnable != null) penaltyHandler.removeCallbacks(penaltyRunnable);
        stopAlertService();
        violationManager.resetIfExpired();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isFinishingApp = true;
        stopScreenPinning();
        if (isFinishing()) {
            try {
                stopService(new Intent(this, AlertSoundService.class));
            } catch (Exception ignored) { }
        }
        if (penaltyRunnable != null) penaltyHandler.removeCallbacks(penaltyRunnable);
        this.binding = null;
    }

    final class JSBridge {
        @JavascriptInterface
        public final void checkBimasoftExamClient(String str) {}

        @JavascriptInterface
        public final boolean isAppPinned() { return pinned; }

        @JavascriptInterface
        public final void startPinningApp() { pinned = true; }

        @JavascriptInterface
        public final void stopPinningApp() { pinned = false; }

        @JavascriptInterface
        public final boolean isDevice() { return false; }

        @JavascriptInterface
        public final boolean isForbidden() {
            return violationManager != null && violationManager.isInPenalty();
        }
    }
}
