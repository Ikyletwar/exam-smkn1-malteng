package com.exam.smkn1maltng;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.exam.smkn1maltng.databinding.ActivityMainBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    // ================================================================
    // FITUR #3: Flag penanda bahwa URL terakhir berasal dari scan QR.
    // Tujuannya: URL hasil scan TIDAK PERNAH tertulis di text box,
    // baik saat ini, maupun saat user kembali dari WebviewActivity.
    // ================================================================
    private boolean cameFromScan = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("setting", 0);

        // Toast credit setiap kali buka
        Toast toast = Toast.makeText(this, R.string.credit, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();

        if (!sharedPreferences.contains("support_zoom")) {
            sharedPreferences.edit().putBoolean("support_zoom", true).apply();
        }

        // ================================================================
        // KEAMANAN: CEK APAKAH ADA URL SISA DARI SEBELUMNYA
        // JIKA ADA, HAPUS AGAR TIDAK BISA DIMANFAATKAN
        // ================================================================
        String url = sharedPreferences.getString("url", null);
        if (url != null) {
            sharedPreferences.edit().remove("url").apply();
            binding.urlEditText.setText("");
        }

        binding.masukBtn.setOnClickListener(v -> {
            String urltv = binding.urlEditText.getText().toString();
            if (urltv.isEmpty()) {
                Toast.makeText(this, R.string.toast_enter_url, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent webAct = new Intent(MainActivity.this, WebviewActivity.class);
            webAct.putExtra("url", urltv);
            // URL disimpan hanya untuk sementara (akan dihapus saat aplikasi ditutup)
            sharedPreferences.edit().putString("url", urltv).apply();
            startActivity(webAct);
        });

        binding.btnScanQR.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                startQRScanner();
            } else {
                requestCameraPermission();
            }
        });

        binding.ivAbout.setOnClickListener(v -> {
            Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(aboutIntent);
        });
    }

    private boolean checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.CAMERA},
            CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                Toast.makeText(this, R.string.qr_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR Code Ujian");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedUrl = result.getContents().trim();
                if (!scannedUrl.startsWith("http://") && !scannedUrl.startsWith("https://")) {
                    scannedUrl = "https://" + scannedUrl;
                }

                // ========================================================
                // FITUR #3: URL scan TIDAK boleh tertulis di text box.
                // 1) Pastikan kosong SEKARANG (defense in depth).
                // 2) Set flag agar onResume juga clear saat kembali
                //    dari WebviewActivity.
                // ========================================================
                binding.urlEditText.setText("");
                cameFromScan = true;

                Intent webAct = new Intent(MainActivity.this, WebviewActivity.class);
                webAct.putExtra("url", scannedUrl);
                // URL disimpan hanya untuk sementara
                sharedPreferences.edit().putString("url", scannedUrl).apply();
                startActivity(webAct);
            } else {
                Toast.makeText(this, R.string.qr_not_read, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ================================================================
    // FITUR #3: onResume — saat user kembali dari WebviewActivity
    // setelah scan, pastikan text box benar-benar kosong.
    // Manual flow (ketik URL lalu Masuk) tidak terpengaruh karena
    // flag cameFromScan = false.
    // ================================================================
    @Override
    protected void onResume() {
        super.onResume();
        if (cameFromScan) {
            binding.urlEditText.setText("");
            cameFromScan = false;
        }
    }

    // ================================================================
    // KEAMANAN: HAPUS URL SAAT APLIKASI DITUTUP
    // ================================================================
    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.edit().remove("url").apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferences.edit().remove("url").apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.edit().remove("url").apply();
        this.binding = null;
    }

    @Override
    public void onBackPressed() {
        sharedPreferences.edit().remove("url").apply();
        finish();
    }
    // ================================================================
    // END KEAMANAN
    // ================================================================
}
