# Meydi AI - Advanced Data Management System (ADMS)
## Arsitektur Sinkronisasi Online, Cache Offline, Auto-Recovery, dan Logging Keamanan

Dokumen komprehensif ini menjelaskan desain sistem, detail implementasi, dokumentasi penggunaan, best practices, dan penanganan kesalahan (error handling) untuk lima pilar utama sistem manajemen data yang tangguh dan aman pada aplikasi Android Meydi AI.

---

## 🚀 1. Gambaran Arsitektur Sistem (System Architecture Overview)

Arsitektur dikembangkan menggunakan prinsip **Clean Architecture** dan **MVVM (Model-View-ViewModel)** dengan alur data satu arah (Unidirectional Data Flow) yang didukung sepenuhnya oleh Jetpack Compose, Room Database, dan Google Firebase.

```
       [ Jetpack Compose UI ] <====== (Flows) ===== [ ViewModel ]
                ||                                       ||
         (User Actions)                            (State Flow)
                ||                                       ||
                \/                                       \/
       [ Repository / Logger ] ===> [ Room Local Cache (Encrypted) ]
                ||                                  ||
          (Sync Queue)                         (PRAGMA Check)
                ||                                  ||
                \/                                  \/
  [ Firebase Cloud Firestore ]             [ SQLite Backup File ]
```

---

## 📦 2. Penjelasan Lima Komponen Utama

### 📡 A. Sinkronisasi Data Online (`com.example.sync`)
- **Engine**: Menggunakan Cloud Firestore untuk real-time listener dan sinkronisasi otomatis.
- **Offline Queue**: Jika perangkat offline, mutasi data disimpan ke tabel `sync_queue` menggunakan `SyncQueueItem`.
- **Auto-Sync on Reconnection**: Menggunakan NetworkCallback dari `ConnectivityManager` untuk mendeteksi perubahan jaringan secara instan dan langsung mengirimkan antrean offline.
- **Conflict Resolution (Server-Wins)**: Saat melakukan push ke Firestore, sistem memeriksa `timestamp` server. Jika data server lebih baru dari data lokal, sistem membatalkan write dan mengadopsi data server untuk mencegah overwriting data yang valid.
- **Background Sync**: Menggunakan **Jetpack WorkManager** (`BackgroundSyncWorker`) untuk menjalankan sinkronisasi terjadwal setiap 15 menit dengan status koneksi `CONNECTED` sebagai syarat wajib.

### 🔒 B. Cache Offline (`com.example.cache`)
- **Engine**: Menggunakan **Room Database** dengan pembatasan entri maksimum (`MAX_CACHE_ENTRIES = 100`) untuk efisiensi penyimpanan memori.
- **Enkripsi AES-256**: Menggunakan cipher dengan mode `AES/GCM/NoPadding` yang didukung oleh kunci aman di Android Keystore System melalui `CryptoUtils`.
- **Kompresi GZIP**: Sebelum dienkripsi, data dikompresi menggunakan `GZIPOutputStream` untuk mengurangi ukuran database secara signifikan hingga 80%, menghemat resource penyimpanan di media Flash.
- **Cache-First Strategy**: Menyediakan helper `getOrFetch` yang memuat data lokal secara instan. Jika cache kosong atau telah kedaluwarsa, helper ini akan menarik data segar dari jaringan, menyimpannya kembali ke cache, lalu memperbarui UI.

### 📂 C. Auto Recovery Data (`com.example.recovery`)
- **Integrity Validation**: Setiap kali aplikasi diluncurkan (On-Launch), sistem menjalankan query `PRAGMA integrity_check` pada database SQLite.
- **Auto Recovery & Rollback**: Jika ditemukan kerusakan berkas (corruption), sistem menutup database, menghapus berkas yang rusak, lalu memulihkan database dari backup valid terakhir (`getLatestValidBackup()`).
- **MD5 Checksum Verification**: Setiap kali berkas backup dibuat, sistem menghitung MD5 hash dari berkas tersebut. Sebelum proses pemulihan (restore) dijalankan, sistem memverifikasi checksum berkas backup saat ini terhadap checksum tercatat untuk menjamin tidak ada manipulasi data eksternal.

### 👥 D. Riwayat Aktivitas (`com.example.activity`)
- **Pencatatan Aktivitas Komprehensif**: Mencatat seluruh aktivitas pengguna yang sensitif seperti pendaftaran (register), masuk (login), keluar (logout), perubahan profil, sinkronisasi data, backup, dan error penting.
- **Metadata**: Menyimpan data `deviceId` (Settings.Secure.ANDROID_ID), alamat IP, status sukses/gagal, deskripsi lengkap, dan penanda waktu (timestamp).
- **Online Backup**: Setiap aktivitas lokal juga dicoba untuk disinkronkan langsung ke sub-koleksi `/users/{userId}/activities` di Firestore.

### 📋 E. Log Sistem Profesional (`com.example.logging`)
- **Severity Levels**: Mendukung tingkat keparahan yang komprehensif (`DEBUG`, `INFO`, `WARN`, `ERROR`, `CRITICAL`).
- **Log Rotation**: Jika ukuran berkas log utama (`meydiai_system_logs.txt`) melebihi 1 MB, log lama akan secara otomatis digeser (renamed) ke berkas cadangan lama, mencegah kehabisan kapasitas media penyimpanan internal (disk space).
- **Format Ekspor Komprehensif**: Mendukung ekspor log ke format teks biasa (`TXT`), objek JSON terformat (`JSON`), dan kompresi GZIP (`.txt.gz`) siap unduh.

---

## 🛠️ 3. Panduan Instalasi & Penggunaan

### 1. Inisialisasi di MainActivity
Sistem ini dirancang untuk diinisialisasi secara instan pada `onCreate()` aplikasi:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Inisialisasi ADMS secara otomatis
    com.example.logging.AppLogger.init(this)
    com.example.activity.ActivityLogger.init(this)
    com.example.cache.CacheManager.init(this)
    com.example.recovery.RecoveryManager.init(this)
    com.example.sync.SyncManager.init(this)
    
    // Jadwalkan Background Sync Worker
    com.example.sync.BackgroundSyncWorker.schedulePeriodicSync(this)
}
```

### 2. Contoh Penggunaan Cache-First
```kotlin
val dataResponse = CacheManager.getOrFetch(
    key = "user_dashboard_data",
    expiryMs = 30 * 60 * 1000, // Kedaluwarsa dalam 30 menit
    fetcher = { apiService.getDashboardData() },
    parser = { rawJson -> parseJsonToModel(rawJson) },
    serializer = { model -> convertModelToJson(model) }
)
```

### 3. Contoh Pencatatan Aktivitas & Log Sistem
```kotlin
try {
    authService.login(email, password)
    ActivityLogger.logActivity(
        name = "USER_LOGIN",
        description = "Pengguna berhasil login ke sistem.",
        isSuccess = true,
        additionalDetails = mapOf("email" to email)
    )
} catch (e: Exception) {
    AppLogger.error("AuthModule", "login", "Gagal melakukan otentikasi login", e)
    ActivityLogger.logActivity(
        name = "USER_LOGIN",
        description = "Gagal login: ${e.localizedMessage}",
        isSuccess = false
    )
}
```

---

## 🛡️ 4. Penanganan Kesalahan (Error Handling) & Best Practices

1. **Pemeriksaan Thread**: Database Room dan pembacaan file diatur secara ketat menggunakan `Dispatchers.IO` guna menghindari freezing atau pemblokiran pada Main Thread (UI thread).
2. **Kekebalan Offline (Offline Resilience)**: Jika Firebase Cloud Firestore tidak tersedia, sistem dengan aman menangkap exception tersebut, menulis entri ke tabel antrean lokal `sync_queue`, dan melanjutkan operasi seolah-olah berhasil di client-side (Optimistic UI update).
3. **Penyimpanan Kunci Keystore**: `CryptoUtils` memuat kunci enkripsi langsung dari Android Keystore Hardware Backed Provider. Kunci tidak pernah disimpan di hardcode, sehingga data sensitif aman meskipun perangkat telah di-root.
