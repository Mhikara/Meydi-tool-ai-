# ☁️ SISTEM AUTO BACKUP CLOUD (KOTLIN & FIREBASE)
### Dokumentasi Integrasi, Aturan Keamanan, dan Arsitektur Sinkronisasi

Sistem ini didesain secara modular menggunakan arsitektur **Offline-First** dengan sinkronisasi real-time, perlindungan enkripsi AES, dan konsol debugging visual langsung di perangkat Android.

---

## 📂 1. Struktur Folder Proyek (Modular)

Arsitektur aplikasi dibagi menjadi beberapa modul fungsional untuk memudahkan pengembangan dan pemeliharaan:

```text
app/src/main/java/com/example/
├── api/
│   └── GeminiGenerator.kt           # Modul AI Generator
├── db/
│   ├── AppDatabase.kt               # Definisi Room Database
│   ├── ProjectDraft.kt              # Data Entity (Draf Proyek)
│   ├── DraftDao.kt                  # Query Database (SQLite)
│   └── DraftRepository.kt           # Repositori Data terintegrasi
├── utils/
│   ├── FirebaseManager.kt           # Konfigurasi & Manajer Layanan Firebase
│   ├── Cryptographer.kt            # Utilitas Enkripsi AES (CBC/PKCS5)
│   ├── BackupLogger.kt              # Logger Aktivitas Backup Terjadwal (Debugging)
│   └── NetworkMonitor.kt            # Sistem Monitoring Konektivitas Real-time
├── viewmodel/
│   └── WorkspaceViewModel.kt        # State Holder, Sinkronisasi Otomatis, & Resolusi Konflik
└── ui/
    └── MeydiAiApp.kt                # Jetpack Compose UI (Workspace, Konsol Backup, Dialog Konflik)
```

---

## 🔐 2. Aturan Keamanan Firebase (Security Rules)

Aturan keamanan (Security Rules) diletakkan di Firestore untuk memastikan **setiap pengguna hanya dapat membaca dan menulis data draf miliknya sendiri**, mencegah kebocoran data antar-pengguna.

Terapkan aturan berikut pada panel **Firestore Security Rules**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Aturan untuk koleksi draf proyek
    match /project_drafts/{documentId} {
      
      // Ambil userEmail dari ID dokumen: formatnya adalah "${email.replace(".", "_")}_${type}"
      // Dan pastikan user yang terautentikasi hanya bisa mengakses data yang sesuai dengan email mereka
      allow read, write: if request.auth != null && 
        (documentId.startsWith(request.auth.token.email.replace(".", "_") + "_") || 
         resource.data.userEmail == request.auth.token.email ||
         request.resource.data.userEmail == request.auth.token.email);
    }
  }
}
```

---

## ⚙️ 3. Konfigurasi Firebase (Client-Side)

Agar aplikasi terhubung ke proyek Firebase Anda, ikuti langkah-langkah berikut:

1. Buat proyek baru di [Firebase Console](https://console.firebase.google.com/).
2. Daftarkan aplikasi Android Anda dengan paket namespace: `com.example` atau applicationId Anda (`com.aistudio.meydiai.vgxtf`).
3. Unduh file `google-services.json` dan letakkan di direktori `/app/google-services.json`.
4. Jika `google-services.json` belum dikonfigurasi, sistem kami secara otomatis mengaktifkan **Fallback Mode** (Draf disimpan secara lokal di SQLite dengan status "Pending Sync") agar aplikasi tetap berjalan lancar tanpa crash.

---

## 🛠️ 4. Contoh Skema Database

### A. Skema Penyimpanan Lokal (SQLite via Room)
Tabel `project_drafts` di SQLite menyimpan draf sementara secara offline:
| Kolom | Tipe Data | Deskripsi |
| :--- | :--- | :--- |
| `id` | INTEGER (PK) | Auto-increment ID draf lokal |
| `type` | TEXT | Kategori draf (e.g. "REMOTION", "CLIPPER") |
| `projectTitle` | TEXT | Nama atau judul draf |
| `promptInput` | TEXT | Input teks prompt (Lokal) |
| `codeContent` | TEXT | Kode script hasil generate (Lokal) |
| `userEmail` | TEXT | Email pemilik data (Kunci Isolasi) |
| `timestamp` | INTEGER | Waktu edit terakhir perangkat (Epoch MS) |
| `isSynced` | INTEGER (Boolean)| Penanda sinkronisasi: `1` (Sudah Sync), `0` (Perlu Sync) |
| `lastCloudTimestamp`| INTEGER | Waktu draf berhasil diunggah ke Firebase |

### B. Skema Penyimpanan Cloud (Firebase Firestore)
Dokumen disimpan di koleksi `project_drafts` dengan ID dokumen `${email}_${type}`:
```json
{
  "type": "REMOTION",
  "projectTitle": "Video Generator Promosi",
  "promptInput": "U2FsdGVkX19H... [Teks Prompt Terenkripsi AES]",
  "codeContent": "QWVzR2NtRW5j... [Kode Script Terenkripsi AES]",
  "selectedTemplateId": "cyberpunk_neon",
  "userEmail": "meydihikara@gmail.com",
  "timestamp": 1782416525000,
  "isEncrypted": true
}
```

---

## 🔄 5. Alur Sinkronisasi & Resolusi Konflik (Conflict Resolution)

Sistem mengadopsi tiga mekanisme utama untuk mematangkan alur sinkronisasi:

1. **Auto Save & Backup Terjadwal (Debounce 1 Detik):**
   Saat pengguna mengetik prompt atau mengedit kode di editor, sistem menunda penyimpanan selama 1 detik (debounce) untuk efisiensi CPU, lalu melakukan autosave ke Room SQLite. Jika internet aktif, data tersebut langsung diunggah terenkripsi ke Cloud Firestore.

2. **Auto Restore saat Login Baru:**
   Jika pengguna login di perangkat baru (di mana Room SQLite masih kosong) namun draf Cloud untuk email tersebut tersedia, sistem secara otomatis melakukan *Restore* secara instan.

3. **Resolusi Konflik Berbasis Waktu (Timestamp-Based):**
   Jika draf lokal dan draf cloud memiliki perbedaan waktu (misal diedit offline di perangkat A, lalu diedit online di perangkat B), sistem tidak akan menimpa data secara sepihak. Aplikasi akan menampilkan **Interactive Conflict Dialog**:
   * **Gunakan Versi Lokal:** Menimpa draf cloud dengan perubahan lokal terbaru Anda.
   * **Gunakan Versi Cloud:** Mengunduh dan menimpa draf lokal dengan versi server terbaru.

---

## 📈 6. Fitur Keamanan & Debugging Terpadu

* **Enkripsi AES-CBC:** Data prompt dan source-code sensitif dienkripsi penuh menggunakan kunci dinamis/aman sebelum dikirim ke Firebase Firestore, menjaga kerahasiaan kekayaan intelektual (IP) pengguna.
* **Backup Console UI:** Panel terminal di dalam workspace yang menampilkan log aktivitas pencadangan secara real-time (Waktu, Kategori Log seperti `[INFO]`, `[SUCCESS]`, `[ERROR]`, dan `[SYNC]`).
* **Retry Otomatis:** Detektor internet memantau status jaringan menggunakan `ConnectivityManager`. Begitu jaringan pulih, antrean offline langsung diunggah secara otomatis tanpa campur tangan pengguna.
