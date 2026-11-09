package com.exam.smkn1maltng;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.exam.smkn1maltng.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ============================================================
        // FASE 2: SETTINGSACTIVITY DI-NON-FUNGSIKAN
        // Langsung finish agar tidak bisa diakses.
        // Seluruh kode asli di bawah di-COMMENT (bukan dihapus)
        // agar bisa di-uncomment di kemudian hari.
        // ============================================================
        finish();
        // ============================================================
        // END FASE 2 (KODE ASLI DI BAWAH DI-COMMENT)
        // ============================================================

        /*
         * // ============================================================
         * // KODE ASLI SETTINGSACTIVITY (DI-COMMENT UNTUK FASE 2)
         * // UNCOMMENT SEMUA KODE DI BAWAH INI UNTUK MENGAKTIFKAN KEMBALI
         * // ============================================================
         * binding = ActivitySettingsBinding.inflate(getLayoutInflater());
         * setContentView(binding.getRoot());
         * 
         * binding.stpvFortune.startAnimation(0, 1);
         * 
         * new ColorfulTextViewAnimator(binding.manual).startAnimationTwo();
         * new ColorfulTextViewAnimator(binding.fitur).startAnimation();
         * 
         * ActionBar supportActionBar = getSupportActionBar();
         * if (supportActionBar != null) {
         * supportActionBar.hide();
         * }
         * 
         * sharedPreferences = getSharedPreferences("setting", 0);
         * if(sharedPreferences.getBoolean("darkmode",false)){
         * binding.switchDarkMode.setChecked(true);
         * } else {
         * binding.switchDarkMode.setChecked(false);
         * }
         * 
         * if(sharedPreferences.getBoolean("support_zoom",true)){
         * binding.switchSupportZoom.setChecked(true);
         * } else {
         * binding.switchSupportZoom.setChecked(false);
         * }
         * 
         * if(sharedPreferences.getString("user_agent", null) != null){
         * binding.etUserAgent.setText(sharedPreferences.getString("user_agent", null));
         * }
         * 
         * binding.switchDarkMode.setOnCheckedChangeListener(new
         * CompoundButton.OnCheckedChangeListener() {
         * 
         * @Override
         * public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
         * if (isChecked) {
         * sharedPreferences.edit().putBoolean("darkmode", true).commit();
         * } else {
         * sharedPreferences.edit().putBoolean("darkmode", false).commit();
         * }
         * }
         * });
         * 
         * binding.switchSupportZoom.setOnCheckedChangeListener(new
         * CompoundButton.OnCheckedChangeListener() {
         * 
         * @Override
         * public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
         * if (isChecked) {
         * sharedPreferences.edit().putBoolean("support_zoom", true).commit();
         * } else {
         * sharedPreferences.edit().putBoolean("support_zoom", false).commit();
         * }
         * }
         * });
         * 
         * binding.btnSaveUserAgent.setOnClickListener((View v) -> {
         * sharedPreferences.edit().putString("user_agent",
         * binding.etUserAgent.getText().toString()).commit();
         * Toast.makeText(this, "User Agent saved", 1).show();
         * });
         * // ============================================================
         * // END KODE ASLI
         * // ============================================================
         */
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}