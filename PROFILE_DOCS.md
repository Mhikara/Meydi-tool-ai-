# 👤 SISTEM PROFIL PENGGUNA MANDIRI (OFFLINE-FIRST & CLOUD-SYNC)
### Dokumentasi Struktur, Konfigurasi Keamanan, dan Panduan Integrasi

Sistem ini didesain secara modular, aman, dan responsif menggunakan arsitektur **Offline-First** untuk menjamin data profil pengguna selalu dapat dibuka kapan pun walau tanpa koneksi internet, dan disinkronkan secara otomatis begitu internet kembali aktif.

---

## 📂 1. Struktur Folder Proyek (Modular)

Implementasi fitur ini tersebar ke dalam modul-modul terstruktur di bawah `/app/src/main/java/com/example/`:

```text
app/src/main/java/com/example/
├── utils/
│   ├── FirebaseManager.kt           # Manajer Utama Firebase (Auth, Firestore, Profil model)
│   └── NetworkMonitor.kt            # Detektor Status Koneksi Real-time
├── ui/
│   └── UserProfileScreen.kt         # Jetpack Compose UI (Tampilan Profil, Edit Form, Dialog Keamanan, Log Sinkronisasi)
```

---

## 🔐 2. Aturan Keamanan Firebase (Security Rules)

Terapkan aturan keamanan berikut di panel **Firestore Security Rules** Anda. Aturan ini memastikan **setiap pengguna terautentikasi hanya dapat membaca dan menulis dokumen profil miliknya sendiri**, mengisolasi data secara total:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Aturan isolasi data dokumen profil pengguna
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

---

## 🛠️ 3. Fitur Utama Sistem Profil

1. **Desain AMOLED Dark & Glowing Neon Accent:**
   Layout visual bertemakan Dark Cyberpunk yang didesain menggunakan palet warna `MidnightBg`, `NeonTeal`, `NeonPurple`, dan `ErrorRed` yang konsisten dengan identitas aplikasi Meydi AI.

2. **Sistem Offline Caching Otomatis:**
   * **Membaca:** Saat memuat profil, sistem pertama-tama membaca dari cache lokal (SharedPreferences). Jika ada koneksi internet, sistem melakukan sinkronisasi dengan Cloud Firestore dan memperbarui cache lokal secara senyap.
   * **Menulis:** Saat menyimpan, data langsung diperbarui di cache lokal terlebih dahulu (respons instan). Jika online, data langsung diunggah ke Firestore. Jika offline, data ditandai dengan flag `pending_profile_sync` di perangkat.

3. **Background Auto Sync:**
   Menggunakan `NetworkMonitor` berbasis Flow yang mendeteksi kapan perangkat terhubung kembali ke internet. Begitu internet aktif, sistem secara otomatis mengunggah perubahan profil yang tertunda ke Cloud Firestore di latar belakang.

4. **Validasi Data Berbasis Client:**
   * Nama Lengkap minimal 3 karakter (tidak boleh kosong).
   * Username minimal 3 karakter alfanumerik (hanya boleh huruf, angka, dan `_`).
   * Nomor HP harus berupa angka digital yang valid (opsional menggunakan awalan `+`).

5. **Preset Avatar Futuristik & Media Picker:**
   Selain mendukung integrasi kamera dan galeri untuk foto profil kustom, sistem juga menyediakan baris **Preset AI Avatar** dengan visual seni cyberpunk yang memukau untuk personalisasi instan.

6. **Keamanan Kredensial Langsung:**
   * **Ubah Password:** Dialog interaktif terenkripsi yang langsung memperbarui sandi login ke server Firebase Auth secara aman.
   * **Verifikasi Email:** Status lencana pintar (VERIFIED/UNVERIFIED). Jika belum terverifikasi, pengguna dapat langsung mengklik lencana tersebut untuk memicu email verifikasi resmi dari Firebase.

7. **Developer Log Terminal Terintegrasi:**
   Terminal mini interaktif di bagian bawah layar yang mencatat semua operasi di level sistem (`[INFO]`, `[SUCCESS]`, `[ERROR]`, `[SYNC]`) lengkap dengan timestamp milidetik, membantu developer melakukan debugging real-time.

---

## 📊 4. Skema Data Profil Pengguna

Berikut adalah format dokumen profil `UserProfile` yang disimpan di Cloud Firestore (koleksi `users` dengan ID dokumen setara `uid` pengguna):

```json
{
  "uid": "Uv8kXp9qZrWsTuVwXyZ123456789",
  "fullName": "Meydi Hikara",
  "username": "meydi_hikara",
  "email": "meydihikara@gmail.com",
  "phone": "+628123456789",
  "photoUrl": "https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?auto=format&fit=crop&w=150&q=80",
  "bio": "Membangun sistem kecerdasan buatan masa depan yang aman dan responsif.",
  "role": "user",
  "joinDate": 1782416525000,
  "lastLoginDate": 1782416925000
}
```

---

## 🚀 5. Cara Menggunakan & Mengintegrasikan Halaman Ini

Untuk memanggil layar Profil Pengguna ini di dalam sistem navigasi Jetpack Compose Anda, cukup panggil fungsi berikut:

```kotlin
UserProfileScreen(
    onBack = { 
        // Aksi ketika tombol kembali diklik (misal kembali ke Dashboard)
        navController.popBackStack() 
    },
    onLogout = {
        // Aksi setelah keluar akun (misal mengarahkan kembali ke halaman Login)
        navController.navigate("login") {
            popUpTo(0) // Membersihkan backstack navigasi
        }
    }
)
```
