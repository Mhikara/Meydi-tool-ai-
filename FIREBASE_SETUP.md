# Panduan Instalasi dan Konfigurasi Firebase untuk Aplikasi MeydiAI

Sistem Otentikasi dan Database Modern telah diintegrasikan ke dalam aplikasi. Karena Firebase membutuhkan file konfigurasi yang bersifat rahasia dan spesifik untuk project Anda (`google-services.json`), Anda perlu mengikuti langkah-langkah di bawah ini untuk menghubungkan aplikasi Android Anda dengan Firebase Project.

## Langkah 1: Buat Project Firebase
1. Buka [Firebase Console](https://console.firebase.google.com/).
2. Klik **"Add project"** (Tambahkan Proyek).
3. Masukkan nama proyek (misalnya: `MeydiAI-App`).
4. (Opsional) Aktifkan Google Analytics, lalu klik **"Create project"**.

## Langkah 2: Daftarkan Aplikasi Android
1. Di halaman Overview Firebase, klik ikon **Android** untuk menambahkan aplikasi.
2. Masukkan **Android package name**: `com.example` (atau sesuaikan dengan applicationId di `app/build.gradle.kts`).
3. (Opsional) Masukkan App nickname.
4. (Penting untuk Google Sign-In & Phone Auth) Masukkan **Debug signing certificate SHA-1**. Anda bisa mendapatkan SHA-1 dengan menjalankan perintah ini di terminal Android Studio:
   `./gradlew signingReport`
5. Klik **"Register app"**.

## Langkah 3: Unduh google-services.json
1. Unduh file `google-services.json` yang diberikan oleh Firebase.
2. Letakkan file `google-services.json` di dalam folder `app/` di proyek Android Anda. (Sejajar dengan `build.gradle.kts` module app).

## Langkah 4: Aktifkan Firebase Authentication
1. Di Firebase Console, navigasi ke menu **Build > Authentication**.
2. Klik **"Get started"**.
3. Pindah ke tab **"Sign-in method"**.
4. Aktifkan provider berikut:
   - **Email/Password**: Aktifkan "Email/Password". (Opsional: Aktifkan "Email link" jika diperlukan).
   - **Google**: Aktifkan dan pilih Support email proyek Anda.
   - **Phone**: Aktifkan untuk memungkinkan Login dengan Nomor HP.

## Langkah 5: Aktifkan Firestore Database & Aturan Keamanan (Security Rules)
1. Di Firebase Console, navigasi ke menu **Build > Firestore Database**.
2. Klik **"Create database"**.
3. Pilih **"Start in production mode"**, pilih region (contoh: `asia-southeast2` untuk Jakarta), lalu klik Create.
4. Buka tab **"Rules"** di Firestore, lalu tempel kode aturan keamanan berikut:

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // Fungsi untuk mengecek apakah user sudah login
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Fungsi untuk mengecek apakah user mengakses datanya sendiri
    function isOwner(userId) {
      return request.auth.uid == userId;
    }
    
    // Fungsi untuk mengecek apakah user adalah admin
    function isAdmin() {
      return isAuthenticated() && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Fungsi untuk mengecek apakah user adalah owner
    function isOwnerRole() {
      return isAuthenticated() && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'owner';
    }

    // Rules untuk koleksi 'users'
    match /users/{userId} {
      // Siapa saja yang login bisa membaca profil dasar
      allow read: if isAuthenticated();
      // User hanya bisa membuat dan mengedit datanya sendiri, admin/owner bisa edit semua
      allow create: if isAuthenticated() && isOwner(userId);
      allow update: if isAuthenticated() && (isOwner(userId) || isAdmin() || isOwnerRole());
      allow delete: if isOwnerRole();
    }
  }
}
```
5. Klik **"Publish"**.

## Langkah 6: Update `build.gradle.kts` (Project Level)
Buka file `build.gradle.kts` (berada di root proyek, BUKAN di dalam folder `app`), pastikan Anda menambahkan plugin `google-services`:

```kotlin
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
    }
}
// ATAU di bagian plugins (jika menggunakan DSL plugins):
plugins {
    id("com.google.gms.google-services") version "4.4.1" apply false
}
```

## Langkah 7: Update `app/build.gradle.kts` (App Level)
Buka file `app/build.gradle.kts`, dan tambahkan apply plugin google services di bagian atas:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // TAMBAHKAN INI
}
```

## Langkah 8: Build dan Jalankan Aplikasi
Setelah menambahkan `google-services.json` dan plugin di `build.gradle.kts`, jalankan "Sync Project with Gradle Files" dan jalankan aplikasi. Sistem Login, Register, Profil, dan sinkronisasi database sudah siap digunakan!
