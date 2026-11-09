package com.exam.smkn1maltng;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * ViolationManager - Mengelola penalti pelanggaran aplikasi ujian.
 * Data disimpan di SharedPreferences agar persist meskipun HP direstart.
 * 
 * Pelanggaran bertingkat:
 * - Pelanggaran 1: 3 menit
 * - Pelanggaran 2: 6 menit
 * - Pelanggaran 3: 12 menit
 * - Pelanggaran 4: 24 menit
 * - Pelanggaran 5+: 48 menit
 */
public class ViolationManager {
    
    private static final String PREF_NAME = "exam_violations";
    private static final String KEY_VIOLATION_COUNT = "violation_count";
    private static final String KEY_PENALTY_END_TIME = "penalty_end_time";
    
    private final SharedPreferences prefs;
    
    // Durasi penalti dalam menit untuk setiap pelanggaran
    private static final int[] PENALTY_DURATIONS = {3, 6, 12, 24, 48};
    
    public ViolationManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Mencatat pelanggaran baru dan memperbarui masa penalti.
     */
    public void recordViolation() {
        int count = getViolationCount() + 1;
        int durationMinutes = getPenaltyDuration(count);
        long endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        prefs.edit()
            .putInt(KEY_VIOLATION_COUNT, count)
            .putLong(KEY_PENALTY_END_TIME, endTime)
            .apply();
    }
    
    /**
     * Mengembalikan apakah pengguna sedang dalam masa penalti.
     */
    public boolean isInPenalty() {
        long endTime = prefs.getLong(KEY_PENALTY_END_TIME, 0);
        return System.currentTimeMillis() < endTime;
    }
    
    /**
     * Mengembalikan sisa waktu penalti dalam detik.
     */
    public long getRemainingTimeInSeconds() {
        if (!isInPenalty()) {
            return 0;
        }
        long endTime = prefs.getLong(KEY_PENALTY_END_TIME, 0);
        return (endTime - System.currentTimeMillis()) / 1000L;
    }
    
    /**
     * Mengembalikan durasi penalti untuk pelanggaran ke-n.
     */
    private int getPenaltyDuration(int violationCount) {
        int index = Math.min(violationCount - 1, PENALTY_DURATIONS.length - 1);
        return PENALTY_DURATIONS[index];
    }
    
    /**
     * Mengembalikan jumlah pelanggaran yang tercatat.
     */
    public int getViolationCount() {
        return prefs.getInt(KEY_VIOLATION_COUNT, 0);
    }
    
    /**
     * Mengembalikan waktu berakhir penalti (milliseconds).
     */
    public long getPenaltyEndTime() {
        return prefs.getLong(KEY_PENALTY_END_TIME, 0);
    }
    
    /**
     * Menghapus semua data pelanggaran (reset).
     */
    public void resetViolations() {
        prefs.edit()
            .remove(KEY_VIOLATION_COUNT)
            .remove(KEY_PENALTY_END_TIME)
            .apply();
    }
    
    /**
     * Mengecek apakah masa penalti sudah lewat, jika ya reset violation count.
     * Dipanggil saat aplikasi dibuka kembali.
     */
    public void resetIfExpired() {
        if (!isInPenalty()) {
            // Jika penalti sudah habis, reset count ke 0 (opsional)
            // Tapi kita biarkan count tetap, karena pelanggaran tidak hilang total
            // Hanya waktu penalti yang habis.
            // Kita tetap simpan count untuk history.
            // Untuk saat ini tidak melakukan reset otomatis.
        }
    }
    
    /**
     * Mendapatkan teks deskripsi pelanggaran untuk ditampilkan di overlay.
     */
    public String getViolationDescription() {
        int count = getViolationCount();
        if (count == 0) {
            return "Belum ada pelanggaran";
        }
        return "Pelanggaran ke-" + count;
    }
}
