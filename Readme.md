# Exam SMKN 1 Malteng

> A hardened web-exam client for **SMKN 1 Malteng** — designed and built by **Hizkia Letwar**.

[![Build](https://github.com/Ikyletwar/exam-smkn1-malteng/actions/workflows/build.yml/badge.svg)](https://github.com/Ikyletwar/exam-smkn1-malteng/actions/workflows/build.yml)
[![Downloads](https://img.shields.io/github/downloads/Ikyletwar/exam-smkn1-malteng/total)](https://github.com/Ikyletwar/exam-smkn1-malteng/releases)

---

## Overview

Exam SMKN 1 Malteng is an Android app that wraps a web-based exam (CBT) portal into a native client, built with anti-cheat in mind. Designed and developed by **Hizkia Letwar** for internal use at SMKN 1 Malteng.

The app is **not** a browser. It is a controlled kiosk-like shell that:

- loads the exam URL (from scan QR or manual input),
- keeps the screen on, locked in place, and screenshot-protected,
- detects every attempt to leave the app,
- punishes violations with escalating time penalties and a loud, non-dismissible alarm,
- keeps the alarm running even when the app is backgrounded.

---

## Features

- **QR-driven entry** — exam URL is delivered by teacher's QR code. Scanned URLs are never shown in the input box.
- **Foreground alarm** — violation alarm keeps playing even when the user presses Home, opens another app, or locks the screen. Runs as a foreground service.
- **Volume lock** — while a penalty is active, the media volume is forced to maximum every 500 ms. User cannot silence the alarm by lowering volume.
- **Escalating penalties** — violation 1 → 3 min, 2 → 6 min, 3 → 12 min, 4 → 24 min, 5+ → 48 min.
- **Lock task mode** — `startLockTask()` pins the app. Users cannot switch apps during the exam.
- **Screenshot protection** — `FLAG_SECURE` blocks screenshots and screen recording.
- **Anti-copy JS** — `userSelect`, `copy`, `paste`, `cut` are disabled inside the WebView.
- **Navigation guard** — hardware back button is intercepted. Long-press back opens the exit dialog.
- **Persistent violation state** — stored in `SharedPreferences` so it survives force-stop and reboot.

---

## Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Java 11 |
| UI | XML layouts, Material Components |
| Web container | Android `WebView` |
| Persistence | `SharedPreferences` |
| Background playback | Foreground `Service` + `MediaPlayer` |
| QR scanning | `journeyapps:zxing-android-embedded` |
| Build | Gradle 8.10 (Kotlin DSL), JDK 17 |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 33 (Android 13) |

---

## Project Structure

```
Exam-SMKN1Malteng/
├── app/
│   └── src/main/
│       ├── java/com/exam/smkn1maltng/
│       │   ├── SplashActivity.java       # launcher, 2.2 s animation
│       │   ├── MainActivity.java         # URL input + QR scan
│       │   ├── WebviewActivity.java      # exam shell, violation detector
│       │   ├── AboutActivity.java        # app info
│       │   ├── SettingsActivity.java     # disabled (phase 2)
│       │   ├── ViolationManager.java     # penalty state
│       │   ├── AlertSoundService.java    # foreground alarm + volume lock
│       │   ├── ColorfulTextViewAnimator.java
│       │   ├── hanks/htextview/...       # RainbowTextView
│       │   └── yanzhikai/textpath/...    # SyncTextPathView
│       ├── res/                          # layouts, drawables, strings
│       └── assets/
│           └── alert_sound.mp3           # alarm sound
├── gradle.properties                     # keystore config (NOT committed)
├── app/exam-release.keystore             # release keystore (gitignored)
└── build.gradle.kts
```

---

## Getting Started

### Prerequisites

- JDK 17
- Android SDK (API 33)
- Gradle 8.10 (wrapper included)
- A device or emulator running Android 5.0+

### Build (debug)

```bash
./gradlew --no-daemon clean assembleDebug
```

APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Build (release)

```bash
./gradlew --no-daemon clean assembleRelease
```

Requires `EXAM_STORE_FILE`, `EXAM_STORE_PASSWORD`, `EXAM_KEY_ALIAS`, `EXAM_KEY_PASSWORD` in `gradle.properties`.

### Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Keystore Setup

The release keystore is **not** committed. To build release locally:

1. Generate a keystore:

   ```bash
   keytool -genkeypair -v \
     -keystore app/exam-release.keystore \
     -alias examkey \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=Your Name, O=Your Org, C=ID"
   ```

2. Add config to `gradle.properties` (root, not `app/`):

   ```properties
   EXAM_STORE_FILE=exam-release.keystore
   EXAM_STORE_PASSWORD=your-strong-password
   EXAM_KEY_ALIAS=examkey
   EXAM_KEY_PASSWORD=your-strong-password
   ```

3. Keep `gradle.properties` and `*.keystore` in `.gitignore`.

> **Never commit your keystore.** A leaked keystore + password lets anyone sign APKs as you.

---

## How It Works

### Entry flow

```
SplashActivity  →  MainActivity  →  WebviewActivity
   (2.2 s)          (QR/URL)         (exam)
```

### Violation flow

```
User leaves app  →  onWindowFocusChanged(false) / onPause()
                 →  recordViolationAndPenalty()
                 →  ViolationManager.recordViolation()
                 →  penalty overlay + AlertSoundService
                 →  timer runs → volume locked at max
                 →  timer expires → alarm stops → volume normal
```

### QR URL hiding

`MainActivity` clears the URL text box twice:
1. immediately after a successful scan,
2. again in `onResume()` if `cameFromScan == true`.

Manual-typed URLs are preserved.

---

## Anti-Cheat Design

The app relies on **three layers** of defense:

| Layer | Mechanism |
|-------|-----------|
| Technical | Volume lock, background alarm, foreground service, `FLAG_SECURE`, lock task mode, QR URL hiding |
| Procedural | Exam URL only obtainable from teacher's QR — asking for a new QR after a violation is socially visible |
| Social | Loud alarm draws the room's attention; violations are audibly announced |

**Known limitations**: clearing app data removes the local violation counter. This is mitigated by the QR gate — a student who clears data must ask the teacher for a new QR, which reveals they were flagged.

---

## Testing Checklist

| # | Scenario | Expected |
|---|----------|----------|
| 1 | Enter exam, press Home | Penalty overlay + alarm + notification |
| 2 | While alarm active, open another app | Alarm continues |
| 3 | Lower volume with hardware button | Volume back to max within 0.5 s |
| 4 | Return to app | Timer running, alarm still playing |
| 5 | Wait for timer to expire | Alarm stops, volume no longer locked |
| 6 | Scan QR, enter exam, go back | Text box empty |
| 7 | Type URL manually, enter, go back | Text box preserved |
| 8 | Exit exam via dialog | Alarm stops, service killed |

---

## Limitations

- **Android 13+**: notification permission (`POST_NOTIFICATIONS`) is declared but not requested at runtime. The alarm still plays; only the notification may be hidden.
- **Aggressive OEMs** (Xiaomi/Oppo/Vivo): may kill the foreground service. Users should whitelist the app in battery settings.
- **Kiosk mode** (`startLockTask`) requires device owner or user confirmation.

---

## Third-Party Libraries

This project uses the following open-source libraries:

- [Totond / TextPathView](https://github.com/totond/TextPathView) — text-on-path animation
- [Hanks-zyh / HTextView](https://github.com/hanks-zyh/HTextView) — animated text views
- [GrenderG / Toasty](https://github.com/GrenderG/Toasty) — toast styling
- [journeyapps / zxing-android-embedded](https://github.com/journeyapps/zxing-android-embedded) — QR scanner

All third-party libraries are used under their respective open-source licenses.

---

## Author

**Hizkia Letwar** — SMKN 1 Malteng, Class XI TJKT-1.  
Designed, written, and maintained by the author.

---

## License

MIT. Third-party libraries retain their own licenses.

---

## Disclaimer

This app is for **internal use at SMKN 1 Malteng**. The author is not responsible for misuse, data loss, or any consequences arising from using or modifying this software.

---

# Bahasa Indonesia

> Klien ujian berbasis web yang diperkeras untuk **SMKN 1 Malteng** — dirancang dan dibangun oleh **Hizkia Letwar**.

[![Build](https://github.com/Ikyletwar/exam-smkn1-malteng/actions/workflows/build.yml/badge.svg)](https://github.com/Ikyletwar/exam-smkn1-malteng/actions/workflows/build.yml)
[![Downloads](https://img.shields.io/github/downloads/Ikyletwar/exam-smkn1-malteng/total)](https://github.com/Ikyletwar/exam-smkn1-malteng/releases)

---

## Ringkasan

Exam SMKN 1 Malteng adalah aplikasi Android yang membungkus portal ujian berbasis web (CBT) menjadi klien native, dibangun dengan fokus anti-kecurangan. Dirancang dan dikembangkan oleh **Hizkia Letwar** untuk penggunaan internal di SMKN 1 Malteng.

Aplikasi ini **bukan** browser. Ini adalah shell terkontrol ala kiosk yang:

- memuat URL ujian (dari scan QR atau input manual),
- menjaga layar tetap menyala, terkunci, dan terlindungi dari screenshot,
- mendeteksi setiap upaya keluar dari aplikasi,
- memberi sanksi pelanggaran dengan penalti waktu bertingkat dan alarm keras yang tidak bisa dibungkam,
- menjaga alarm tetap berbunyi bahkan ketika aplikasi di-background.

---

## Fitur

- **Masuk lewat QR** — URL ujian dikirim lewat QR code dari guru. URL hasil scan tidak pernah ditampilkan di text box.
- **Alarm foreground** — alarm pelanggaran tetap berbunyi walau user tekan Home, buka aplikasi lain, atau kunci layar.
- **Kunci volume** — selama penalti aktif, volume media dipaksa maksimum setiap 500 ms. User tidak bisa membungkam alarm.
- **Penalti bertingkat** — pelanggaran 1 → 3 menit, 2 → 6 menit, 3 → 12 menit, 4 → 24 menit, 5+ → 48 menit.
- **Lock task mode** — `startLockTask()` mengunci aplikasi.
- **Perlindungan screenshot** — `FLAG_SECURE` memblokir screenshot dan screen recording.
- **Anti-copy JS** — `userSelect`, `copy`, `paste`, `cut` dinonaktifkan di dalam WebView.
- **Penjaga navigasi** — tombol back hardware dicegat. Long-press back membuka dialog keluar.
- **Status pelanggaran persisten** — disimpan di `SharedPreferences`.

---

## Stack Teknologi

| Lapisan | Pilihan |
|---------|---------|
| Bahasa | Java 11 |
| UI | XML layout, Material Components |
| Kontainer web | Android `WebView` |
| Persistensi | `SharedPreferences` |
| Pemutaran background | Foreground `Service` + `MediaPlayer` |
| Scan QR | `journeyapps:zxing-android-embedded` |
| Build | Gradle 8.10 (Kotlin DSL), JDK 17 |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 33 (Android 13) |

---

## Struktur Proyek

```
Exam-SMKN1Malteng/
├── app/
│   └── src/main/
│       ├── java/com/exam/smkn1maltng/
│       │   ├── SplashActivity.java       # launcher, animasi 2.2 detik
│       │   ├── MainActivity.java         # input URL + scan QR
│       │   ├── WebviewActivity.java      # shell ujian, detektor pelanggaran
│       │   ├── AboutActivity.java        # info aplikasi
│       │   ├── SettingsActivity.java     # dinonaktifkan (fase 2)
│       │   ├── ViolationManager.java     # status penalti
│       │   ├── AlertSoundService.java    # alarm foreground + kunci volume
│       │   ├── ColorfulTextViewAnimator.java
│       │   ├── hanks/htextview/...       # RainbowTextView
│       │   └── yanzhikai/textpath/...    # SyncTextPathView
│       ├── res/                          # layout, drawable, strings
│       └── assets/
│           └── alert_sound.mp3           # suara alarm
├── gradle.properties                     # config keystore (TIDAK di-commit)
├── app/exam-release.keystore             # keystore release (gitignored)
└── build.gradle.kts
```

---

## Memulai

### Prasyarat

- JDK 17
- Android SDK (API 33)
- Gradle 8.10 (wrapper sudah termasuk)
- Device atau emulator Android 5.0+

### Build (debug)

```bash
./gradlew --no-daemon clean assembleDebug
```

APK dihasilkan di `app/build/outputs/apk/debug/app-debug.apk`.

### Build (release)

```bash
./gradlew --no-daemon clean assembleRelease
```

Butuh `EXAM_STORE_FILE`, `EXAM_STORE_PASSWORD`, `EXAM_KEY_ALIAS`, `EXAM_KEY_PASSWORD` di `gradle.properties`.

### Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Setup Keystore

Keystore release **tidak** di-commit. Untuk build release secara lokal:

1. Generate keystore:

   ```bash
   keytool -genkeypair -v \
     -keystore app/exam-release.keystore \
     -alias examkey \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=Nama Kamu, O=Organisasi, C=ID"
   ```

2. Tambahkan config ke `gradle.properties` (di root, bukan `app/`):

   ```properties
   EXAM_STORE_FILE=exam-release.keystore
   EXAM_STORE_PASSWORD=password-kuat-kamu
   EXAM_KEY_ALIAS=examkey
   EXAM_KEY_PASSWORD=password-kuat-kamu
   ```

3. Pastikan `gradle.properties` dan `*.keystore` ada di `.gitignore`.

> **Jangan pernah commit keystore.** Keystore + password yang bocor memungkinkan siapa pun menandatangani APK atas namamu.

---

## Cara Kerja

### Alur masuk

```
SplashActivity  →  MainActivity  →  WebviewActivity
   (2.2 s)          (QR/URL)         (ujian)
```

### Alur pelanggaran

```
User keluar app  →  onWindowFocusChanged(false) / onPause()
                 →  recordViolationAndPenalty()
                 →  ViolationManager.recordViolation()
                 →  overlay penalti + AlertSoundService
                 →  timer berjalan → volume dikunci maksimum
                 →  timer habis → alarm berhenti → volume normal
```

### Penyembunyian URL QR

`MainActivity` membersihkan text box URL dua kali:
1. segera setelah scan sukses,
2. lagi di `onResume()` jika `cameFromScan == true`.

URL yang diketik manual tetap dipertahankan.

---

## Desain Anti-Kecurangan

Aplikasi ini mengandalkan **tiga lapis** pertahanan:

| Lapis | Mekanisme |
|-------|-----------|
| Teknis | Kunci volume, alarm background, foreground service, `FLAG_SECURE`, lock task mode, penyembunyian URL QR |
| Prosedural | URL ujian hanya bisa diperoleh dari QR guru — meminta QR baru setelah pelanggaran akan terlihat secara sosial |
| Sosial | Alarm keras menarik perhatian seisi ruangan; pelanggaran diumumkan secara audio |

**Keterbatasan yang diketahui**: clear data aplikasi menghapus counter pelanggaran lokal. Ini dimitigasi oleh gerbang QR — siswa yang clear data harus meminta QR baru ke guru, yang mengungkap bahwa dia sudah ditandai.

---

## Checklist Pengujian

| # | Skenario | Ekspektasi |
|---|----------|------------|
| 1 | Masuk ujian, tekan Home | Overlay penalti + alarm + notifikasi |
| 2 | Saat alarm aktif, buka aplikasi lain | Alarm tetap berbunyi |
| 3 | Kecilkan volume pakai tombol hardware | Volume balik ke max dalam 0,5 detik |
| 4 | Kembali ke aplikasi | Timer berjalan, alarm masih berbunyi |
| 5 | Tunggu timer habis | Alarm berhenti, volume tidak dikunci lagi |
| 6 | Scan QR, masuk ujian, kembali | Text box kosong |
| 7 | Type URL manual, masuk, kembali | Text box tetap berisi |
| 8 | Keluar ujian lewat dialog | Alarm berhenti, service dimatikan |

---

## Keterbatasan

- **Android 13+**: izin notifikasi (`POST_NOTIFICATIONS`) dideklarasikan tapi tidak diminta saat runtime. Alarm tetap berbunyi; hanya notifikasi yang mungkin tersembunyi.
- **OEM agresif** (Xiaomi/Oppo/Vivo): bisa membunuh foreground service.
- **Kiosk mode** (`startLockTask`) butuh device owner atau konfirmasi user.

---

## Pustaka Pihak Ketiga

Proyek ini menggunakan pustaka open-source berikut:

- [Totond / TextPathView](https://github.com/totond/TextPathView) — animasi teks di jalur
- [Hanks-zyh / HTextView](https://github.com/hanks-zyh/HTextView) — text view beranimasi
- [GrenderG / Toasty](https://github.com/GrenderG/Toasty) — toast styling
- [journeyapps / zxing-android-embedded](https://github.com/journeyapps/zxing-android-embedded) — scanner QR

Semua pustaka pihak ketiga digunakan sesuai lisensi open-source masing-masing.

---

## Penulis

**Hizkia Letwar** — SMKN 1 Malteng, Kelas XI TJKT-1.  
Dirancang, ditulis, dan dipelihara oleh penulis.

---

## Lisensi

MIT. Pustaka pihak ketiga tetap mengikuti lisensi masing-masing.

---

## Disclaimer

Aplikasi ini untuk **penggunaan internal di SMKN 1 Malteng**. Penulis tidak bertanggung jawab atas penyalahgunaan, kehilangan data, atau konsekuensi apa pun yang timbul dari penggunaan atau modifikasi perangkat lunak ini.
