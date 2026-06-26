# RBAC (Role-Based Access Control) System Documentation
## Meydi AI App Security & Access Management Architecture

Dokumentasi komprehensif ini menjelaskan arsitektur, struktur data, integrasi middleware, dan cara penggunaan sistem manajemen hak akses (RBAC) pada aplikasi Android Meydi AI.

---

## 1. Struktur Folder Proyek (Folder Structure)

Struktur modul keamanan RBAC dibuat terpisah secara modular di bawah package `com.example.rbac` untuk menjamin kerapian kode dan kepatuhan terhadap Clean Architecture:

```
app/src/main/java/com/example/
│
├── rbac/
│   ├── model/
│   │   ├── UserRole.kt        # Definisi enumerasi Role (Owner, Admin, User) & warna visual
│   │   ├── UserStatus.kt      # Definisi status akun (Active, Suspended)
│   │   └── RbacUser.kt        # Data model pengguna lengkap (uid, nama, email, role, status, dll.)
│   │
│   ├── repository/
│   │   └── RbacRepository.kt  # Logika pembacaan Firestore, local caching SharedPreferences & update role
│   │
│   ├── middleware/
│   │   └── RbacMiddleware.kt  # Interceptor navigasi berbasis izin akses role & status penangguhan
│   │
│   ├── viewmodel/
│   │   └── RbacViewModel.kt   # Orkestrator status UI, state-flow & trigger simulasi login/sinkronisasi
│   │
│   └── ui/
│       ├── RbacComponents.kt       # Komponen UI reusable (Shimmer loader, CyberDialog, InfoCard)
│       ├── RbacUserDashboardScreen.kt   # Dashboard khusus User (Profil, aktivitas, ganti password)
│       ├── RbacAdminDashboardScreen.kt  # Dashboard Admin (Kelola user, buat pengumuman, statistik)
│       └── RbacOwnerDashboardScreen.kt  # Dashboard Owner (Akses mutlak, backup, restore, audit log)
```

---

## 2. Contoh Struktur Database (Database Schema)

Setiap dokumen user disimpan di Cloud Firestore pada path koleksi `/users/{uid}` dengan skema berikut:

```json
{
  "uid": "Wv87KxMpZq890ABcdEfGh",
  "nama": "Meydi Hikara",
  "email": "meydihikara@gmail.com",
  "role": "owner",
  "status": "active",
  "createdAt": 1782421815000,
  "lastLogin": 1782421832000,
  "photoURL": "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
  "phoneNumber": "+6281234567890"
}
```

---

## 3. Firebase Security Rules (`firestore.rules`)

Aturan keamanan server-side diatur menggunakan Firebase Security Rules berikut untuk mencegah bypass Client-side:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    function isAuthenticated() { return request.auth != null; }
    function getUserData() { return get(/databases/$(database)/documents/users/$(request.auth.uid)).data; }
    
    function isOwner() {
      return isAuthenticated() && (
        getUserData().role == "owner" || 
        request.auth.token.email == "meydihikara@gmail.com"
      );
    }

    function isAdmin() {
      return isAuthenticated() && (
        getUserData().role == "admin" || 
        getUserData().role == "owner" ||
        request.auth.token.email == "meydihikara@gmail.com"
      );
    }

    match /users/{userId} {
      allow read: if isAuthenticated() && (request.auth.uid == userId || isAdmin());
      allow create: if isAuthenticated() && request.auth.uid == userId && request.resource.data.role == "user" && request.resource.data.status == "active";
      allow update: if isAuthenticated() && (
        isOwner() ||
        (isAdmin() && resource.data.role != "owner" && resource.data.role != "admin" && (request.resource.data.role == "admin" || request.resource.data.role == "user")) ||
        (request.auth.uid == userId && resource.data.role == request.resource.data.role && resource.data.status == request.resource.data.status)
      );
      allow delete: if isOwner();
    }
  }
}
```

---

## 4. Alur Integrasi Middleware & Navigasi

Sistem navigasi pada `MeydiAiApp.kt` secara otomatis mengintersept setiap perubahan screen menggunakan `RbacMiddleware`:

1. **Splash/Login Success**: `rbacViewModel.loginSimulatedUser(email)` dipicu.
2. **Local Cache Cek**: Sistem memuat role pengguna dari `SharedPreferences` secara instan agar UI tidak berkedip (no-flicker).
3. **Firestore Sync**: Di latar belakang, sistem mengambil data terbaru dari Cloud Firestore (jika online) dan memperbarui cache lokal secara otomatis.
4. **Access Evaluation**: `RbacMiddleware.evaluateScreenAccess` dijalankan:
   - Jika pengguna bertipe `USER` mencoba membuka `OWNER_DASHBOARD`, middleware memblokir navigasi dan melempar toast peringatan.
   - Jika status akun `suspended`, pengguna otomatis keluar secara paksa (forced logout) dan diarahkan ke halaman login dengan dialog penangguhan.

---

## 5. Fitur Utama Per Dashboard

### 👤 Dashboard User (Regular User)
- **Profil Modern**: Dilengkapi avatar abstrak holografis interaktif.
- **Daftar Aktivitas Terkini**: Menampilkan log login/perubahan profil pengguna yang terenkripsi lokal.
- **Pengaturan Akun**: Opsi mengubah kata sandi dengan modal interaktif yang divalidasi dengan aman.

### 🛡️ Dashboard Admin
- **Statistik Cepat**: Grafik ringkasan jumlah total User, Admin, dan persentase keaktifan.
- **User Directory**: List seluruh pengguna terdaftar dengan fungsionalitas cari instan.
- **Manajemen User Terbatas**: Admin dapat menangguhkan/mengaktifkan kembali akun `USER` biasa (tidak berhak mengubah Admin/Owner).
- **Pengumuman Global**: Membuat pesan sistem global terenkripsi.

### 👑 Dashboard Owner (Akses Mutlak)
- **Owner Control Panel**: Opsi penuh untuk mengedit role pengguna manapun (`USER` <-> `ADMIN`).
- **Global System Config**: Menyesuaikan setelan pemeliharaan sistem, cooldown request, dan pembatasan spam.
- **Backup & Restore**: Simulasi kompresi database lengkap dengan satu ketukan (one-tap) yang memicu visual progress cyberpunk.
- **Audit Logs Explorer**: Memeriksa log aktivitas sensitif seluruh sistem.

---

## 6. Best Practices Keamanan (Security Hardening)

1. **Anti Privilege Escalation**: Data role disimpan di Cloud Firestore yang dikunci rapat dengan Security Rules. Parameter role dikunci di server-side agar tidak bisa diubah oleh manipulasi request client-side.
2. **Offline Resilience**: Cache SharedPreferences terintegrasi memungkinkan otorisasi luring yang tangguh.
3. **Session Revocation**: Jika pengguna ditangguhkan oleh Owner di Firestore, session token akan kedaluwarsa seketika, memaksa pengguna logout demi mencegah pelanggaran data lanjutan.
4. **Debounced UI Actions**: Seluruh tombol mutasi penting dibatasi dengan cooldown 600ms (debouncing) untuk menangkal spamming click yang berlebihan.
