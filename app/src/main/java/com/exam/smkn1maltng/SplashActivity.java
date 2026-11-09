package com.exam.smkn1maltng;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.exam.smkn1maltng.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private static final int SPLASH_DURATION = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.getRoot().post(this::startSplashAnimation);
    }

    private void startSplashAnimation() {
        // ================================================================
        // LOGO: Scale + Fade In (0.6 → 1.0)
        // ================================================================
        ImageView logo = binding.ivLogo;
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.6f, 1.0f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.6f, 1.0f);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logo, "alpha", 0.0f, 1.0f);

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(logoScaleX, logoScaleY, logoAlpha);
        logoAnim.setDuration(700);
        logoAnim.setInterpolator(new AccelerateDecelerateInterpolator());

        // ================================================================
        // TEXT: Fade In (delay)
        // ================================================================
        TextView tvName = binding.tvSchoolName;
        ObjectAnimator textAlpha = ObjectAnimator.ofFloat(tvName, "alpha", 0.0f, 1.0f);
        textAlpha.setDuration(500);
        textAlpha.setStartDelay(300);

        TextView tvSub = binding.tvSubtitle;
        ObjectAnimator subAlpha = ObjectAnimator.ofFloat(tvSub, "alpha", 0.0f, 1.0f);
        subAlpha.setDuration(500);
        subAlpha.setStartDelay(500);

        // ================================================================
        // DOTS: Fade In
        // ================================================================
        LinearLayout dots = binding.llDots;
        ObjectAnimator dotsAlpha = ObjectAnimator.ofFloat(dots, "alpha", 0.0f, 1.0f);
        dotsAlpha.setDuration(400);
        dotsAlpha.setStartDelay(700);

        TextView tvLoading = binding.tvLoading;
        ObjectAnimator loadAlpha = ObjectAnimator.ofFloat(tvLoading, "alpha", 0.0f, 1.0f);
        loadAlpha.setDuration(400);
        loadAlpha.setStartDelay(700);

        // ================================================================
        // FOOTER: Fade In (paling akhir)
        // ================================================================
        TextView tvVersion = binding.tvVersion;
        ObjectAnimator versionAlpha = ObjectAnimator.ofFloat(tvVersion, "alpha", 0.0f, 0.5f);
        versionAlpha.setDuration(400);
        versionAlpha.setStartDelay(900);

        // ================================================================
        // JALANKAN SEMUA ANIMASI BERSAMAAN
        // ================================================================
        AnimatorSet allAnim = new AnimatorSet();
        allAnim.playTogether(logoAnim, textAlpha, subAlpha, dotsAlpha, loadAlpha, versionAlpha);
        allAnim.start();

        // ================================================================
        // ANIMASI DOTS: Naik-turun bergantian (loop)
        // ================================================================
        startDotsAnimation();

        // ================================================================
        // PINDAH KE MAINACTIVITY SETELAH SPLASH_DURATION
        // ================================================================
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }

    private void startDotsAnimation() {
        View dot1 = binding.dot1;
        View dot2 = binding.dot2;
        View dot3 = binding.dot3;

        // Dot 1: turun-naik
        ObjectAnimator dot1Up = ObjectAnimator.ofFloat(dot1, "translationY", 0f, -12f);
        dot1Up.setDuration(400);
        dot1Up.setInterpolator(new AccelerateInterpolator());
        dot1Up.setRepeatMode(ObjectAnimator.REVERSE);
        dot1Up.setRepeatCount(ObjectAnimator.INFINITE);
        dot1Up.start();

        // Dot 2: naik-turun (delay 150ms)
        ObjectAnimator dot2Up = ObjectAnimator.ofFloat(dot2, "translationY", 0f, -12f);
        dot2Up.setDuration(400);
        dot2Up.setStartDelay(150);
        dot2Up.setInterpolator(new AccelerateInterpolator());
        dot2Up.setRepeatMode(ObjectAnimator.REVERSE);
        dot2Up.setRepeatCount(ObjectAnimator.INFINITE);
        dot2Up.start();

        // Dot 3: naik-turun (delay 300ms)
        ObjectAnimator dot3Up = ObjectAnimator.ofFloat(dot3, "translationY", 0f, -12f);
        dot3Up.setDuration(400);
        dot3Up.setStartDelay(300);
        dot3Up.setInterpolator(new AccelerateInterpolator());
        dot3Up.setRepeatMode(ObjectAnimator.REVERSE);
        dot3Up.setRepeatCount(ObjectAnimator.INFINITE);
        dot3Up.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
