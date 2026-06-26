# 🛡️ Sistem Pengaturan Akun Meydi AI (Account Settings System)
### Dokumentasi Teknis, Arsitektur, Desain UI/UX, dan Panduan Keamanan Siber
**Ditulis oleh:** Senior Android Developer, UI/UX Designer, & Security Engineer

---

## 🌌 1. Ikhtisar Sistem (System Overview)
Sistem Pengaturan Akun (Account Settings) pada aplikasi **Meydi AI** dirancang untuk memberikan kendali mutlak kepada pengguna atas identitas digital, preferensi privasi, dan perlindungan keamanan mereka. Sistem ini mengadopsi prinsip **Offline-First**, **AMOLED Dark Cyberpunk Aesthetic**, dan **Zero-Trust Security**.

### Alur Kerja Utama (Workflow)
```
[User Input/Changes] ──► [Local SharedPreferences Cache] (Instant Render)
                                   │
                                   ▼
                       [Network Status Monitor]
                                   │
                ┌──────────────────┴──────────────────┐
                ▼ (Online)                            ▼ (Offline)
     [Cloud Firestore Sync]                  [Pending-Sync Flag Set]
     [Firebase Auth Update]                           │
                │                                     ▼
                └──────────────────────────► [Auto-Sync when Restored]
```

---

## 📂 2. Struktur Folder & Modul Arsitektur
Sistem ini diimplementasikan dengan arsitektur **MVVM (Model-View-ViewModel)** yang bersih, terisolasikan secara modular untuk mendukung kemudahan pemeliharaan:

```
app/src/main/java/com/example/
│
├── ui/
│   ├── UserProfileScreen.kt     # Tampilan Utama Pengaturan Akun (Jetpack Compose)
│   └── MeydiAiApp.kt            # Router & Navigasi Utama Aplikasi
│
├── utils/
│   ├── FirebaseManager.kt       # Penghubung Autentikasi & Firestore SDK
│   ├── NetworkMonitor.kt        # Pemantau Koneksi Internet Real-Time (Coroutines Flow)
│   └── Cryptographer.kt         # Enkripsi AES-CBC 128-bit Sisi Klien (Client-Side)
│
└── model/ (atau didalam service/ SharedPreferences)
    └── AuditActivityLog.kt      # Model Data Log Aktivitas Keamanan Akun
```

---

## 🛠️ 3. Konfigurasi Firebase & Firestore
Aplikasi menggunakan ekosistem Firebase untuk mengelola kredensial dan persistensi awan.

### A. Struktur Database Cloud Firestore
Setiap pengguna memiliki dokumen tersendiri di dalam koleksi `users` yang terisolasi berdasarkan `uid`:

#### Koleksi Utama: `users/{uid}`
```json
{
  "uid": "Wv8KxpZq012345ABCDE",
  "fullName": "Meydi Hikara",
  "username": "meydi_hikara",
  "email": "meydihikara@gmail.com",
  "phone": "+628123456789",
  "photoUrl": "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
  "bio": "Menjelajahi dunia digital AI secara aman.",
  "role": "user",
  "joinDate": 1782417580000,
  "lastLoginDate": 1782417780000
}
```

#### Sub-Koleksi Audit: `users/{uid}/audit_logs/{logId}`
```json
{
  "id": "7f09a8b1-3c2d-4e5f-8a9b-0c1d2e3f4g5h",
  "timestamp": 1782417850000,
  "type": "SECURITY",
  "description": "Mengaktifkan Perlindungan 2FA (Two-Factor Authentication).",
  "isCloudSynced": true
}
```

### B. Firebase Security Rules (Keamanan Mutlak)
Untuk memblokir serangan pembajakan data atau kebocoran akses dari luar, aturan Firestore dikunci menggunakan paradigma **Owner-Only Read/Write**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Aturan untuk dokumen profil dan audit log pengguna
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      match /audit_logs/{logId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

---

## 🔒 4. Strategi Keamanan & Privasi Siber
Sebagai Security Engineer, sistem ini menyematkan perlindungan berlapis terhadap potensi kerentanan:

1. **Re-Authentication Dialog**: Sebelum pengguna dapat melakukan operasi berisiko tinggi (mengubah email, memodifikasi kata sandi, mengekspor berkas data, mengaktifkan 2FA, atau memusnahkan akun), sistem mewajibkan penginputan kembali sandi aktif saat ini guna memverifikasi identitas asli (*Double-Verification*).
2. **Local AES Encryption**: Data teks sensitif diproteksi dengan algoritma **AES-CBC 128-bit** (melalui `Cryptographer.kt`) sebelum diunggah ke transit Firestore untuk meminimalkan risiko pengintipan data (man-in-the-middle).
3. **Double-Confirmation Destruction**: Penghapusan akun permanen membutuhkan input teks konfirmasi persis `MEYDI DELETE` untuk mencegah kecelakaan pengetukan tombol yang tidak disengaja. Seketika disetujui, akun di Firebase Auth, dokumen di Firestore, dan seluruh preferensi lokal di SharedPreferences akan dilenyapkan secara total (*Total Data Wipeout*).
4. **Session Device Termination**: Menampilkan gawai-gawai yang sedang login secara real-time. Pengguna dapat secara paksa memutuskan hubungan (*remote signout*) gawai asing yang mencurigakan secara instan.

---

## 🎨 5. Desain UI/UX & Respon Responsif
Meydi AI menggunakan tema **AMOLED Dark Cyberpunk Theme** yang memanjakan mata, hemat daya, dan modern:
- **Neon Teal (`#00FFCC`)** & **Neon Purple (`#7F00FF`)**: Digunakan sebagai aksen penunjuk navigasi aktif, status sukses, dan tombol aksi utama.
- **Error Red (`#FFFF3366`)**: Memberikan peringatan tegas pada aksi destruktif (hapus akun, logout, putus sesi gawai).
- **Smooth Transition Layout**: Transisi tab menu menggunakan `AnimatedContent` dengan efek fade-in/out yang sangat lembut.
- **Window Size Adaptability**: Antarmuka dioptimalkan dengan pembatas kontainer (`Modifier.widthIn(max = 600.dp)`) agar tetap terlihat anggun dan tidak merenggang saat dibuka pada tablet atau layar lipat (foldable).

---

## 🚀 6. Panduan Instalasi & Penggunaan (How-To-Run)

### Cara Menguji Fitur dalam Aplikasi
1. **Buka Profil**: Tekan ikon profil di sudut kanan atas layar beranda aplikasi Meydi AI.
2. **Indikator Jaringan**: Perhatikan lencana status (Online/Offline) di bar atas yang memantau konektivitas internet secara live.
3. **Ubah Data Profil (Tab Ubah)**: Masukkan nama baru atau bio, ketuk "SIMPAN PERUBAHAN". Jika offline, data akan langsung masuk ke cache. Saat Anda online kembali, sistem mendeteksi koneksi dan melakukan **Auto-Sync** ke awan secara instan!
4. **Keamanan & 2FA (Tab Keamanan)**: 
   - Klik Switch 2FA, sistem akan memicu popup re-authentication. 
   - Masukkan kata sandi Anda. Setelah tervalidasi, Anda akan disuguhkan Setup Authenticator lengkap dengan **Secret Key** dan **Visual Mock QR Code**. 
   - Masukkan kode simulasi `123456` untuk menyalakan 2FA.
5. **Ekspor Data (Tab Lanjutan)**: Ketuk "EKSPOR DATA", sistem akan langsung memformat berkas dump JSON lengkap akun Anda dalam tampilan Terminal Kode Neon yang interaktif. Anda dapat menyalinnya hanya dengan satu ketukan.
6. **Audit Logs (Tab Audit)**: Buka tab audit untuk melihat catatan riwayat modifikasi akun Anda secara kronologis yang rapi.
