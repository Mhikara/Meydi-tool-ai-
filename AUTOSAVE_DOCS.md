# Sistem Auto Save & Sinkronisasi Cloud Real-Time (MeydiAI Subsystem)

Dokumentasi ini menjelaskan arsitektur, struktur data, dan petunjuk penggunaan sistem **Auto Save Data** yang tangguh, modular, offline-first, dan aman untuk aplikasi Android modern menggunakan Kotlin, Jetpack Compose, Room (SQLite), dan Google Firebase Firestore.

---

## 1. Fitur Utama Sistem
- **Real-Time Data Detection**: Mendeteksi perubahan draf penulisan kode atau prompt masukan secara instan dengan debounce 1 detik guna menghemat daya perangkat dan pemrosesan I/O.
- **Offline-First Storage**: Setiap perubahan data disimpan ke dalam database lokal Room (SQLite) terlebih dahulu. Data aman dari force close, kegagalan sistem, maupun perangkat mati mendadak.
- **Automatic Cloud Sync**: Saat perangkat terhubung ke internet, sistem secara real-time mendeteksi status online dan mengunggah draf yang belum sinkron ke Firestore secara background.
- **Interactive Conflict Resolution & Auto Recovery**: Jika draf lokal memiliki waktu edit yang berbeda dengan draf di Cloud, sistem memunculkan dialog pilihan pemulihan cerdas:
  - **Gunakan Versi Lokal (Offline-Secure)**
  - **Gunakan Versi Cloud (Sinkronisasi Server)**
- **L1 Performance Cache**: Dilengkapi dengan *in-memory cache* untuk membandingkan draf sebelum melakukan penulisan disk, menghindari beban operasi tulis (write load) yang tidak diperlukan.
- **Data Validation**: Mencegah penyimpanan draf kosong atau tidak valid.

---

## 2. Struktur Folder Proyek (Modular & Bersih)

Berikut adalah struktur folder implementasi modul Auto Save di aplikasi Android:

```text
/app/src/main/java/com/example/
│
├── MainActivity.kt                 # Entitas Aktivitas utama Android
│
├── db/                             # LAYER DATABASE LOKAL (ROOM)
│   ├── AppDatabase.kt              # Inisialisasi Database Room & Destructive Migration
│   ├── ProjectDraft.kt             # Room Entity Model dengan penanda isSynced & timestamp
│   ├── DraftDao.kt                 # Data Access Object untuk operasi SQL draf proyek
│   └── DraftRepository.kt          # Abstraksi data akses lokal untuk ViewModel
│
├── viewmodel/                      # LAYER VIEWMODEL (MANAJEMEN STATE)
│   └── WorkspaceViewModel.kt       # StateFlow, debounced scheduler, sinkronisasi, & penangan konflik
│
├── utils/                          # LAYER UTILITAS / HELPER
│   ├── NetworkMonitor.kt           # Mendeteksi koneksi internet real-time via ConnectivityManager
│   └── FirebaseManager.kt          # Manajer inisialisasi aman FirebaseAuth & FirebaseFirestore
│
└── ui/                             # LAYER UI (JETPACK COMPOSE)
    ├── MeydiAiApp.kt               # Integrasi Workspace Screen & Dialog Conflict Recovery
    └── UserProfileScreen.kt        # Tampilan profil pengguna terintegrasi
```

---

## 3. Desain Skema Database

### A. Penyimpanan Lokal: Room SQLite Table (`project_drafts`)
Room merepresentasikan tabel penyimpanan lokal dalam perangkat pengguna dengan model berikut:

```kotlin
@Entity(tableName = "project_drafts")
data class ProjectDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,                       // "REMOTION", "CANVAS", dll
    val projectTitle: String,               // Nama draf proyek
    val promptInput: String,                // Masukan prompt teks pengguna
    val codeContent: String,                // Kode editor Remotion (React/JS)
    val selectedTemplateId: String? = null, // ID template terpilih
    val userEmail: String,                  // Email pemilik draf (kunci relasi)
    val timestamp: Long,                    // Waktu edit terakhir (milidetik)
    val isSynced: Boolean = false,          // Penanda sinkronisasi ke cloud
    val lastCloudTimestamp: Long = 0L       // Waktu sinkronisasi cloud terakhir
)
```

### B. Penyimpanan Cloud: Firestore Collection Path (`/project_drafts/{documentId}`)
Dokumen cloud disimpan menggunakan ID dokumen unik gabungan: `${email.replace(".", "_")}_${type}`.

```json
{
  "type": "REMOTION",
  "projectTitle": "Draft REMOTION",
  "promptInput": "Create futuristic cyberpunk animation",
  "codeContent": "import {AbsoluteFill} from 'remotion'...",
  "selectedTemplateId": "cyber_glitch",
  "userEmail": "meydihikara@gmail.com",
  "timestamp": 1782415761000
}
```

---

## 4. Mekanisme Kerangka Kerja Alur Kerja (Workflow)

```text
[ Perubahan Kode/Prompt ]
          │
          ▼
   [ L1 Memory Cache Check ] ──(Sama dengan cache)──> [ Lewatkan Penyimpanan ]
          │
          ▼ (Berbeda)
   [ Atur Status: "Menyimpan..." ]
          │
          ▼
   [ Simpan ke SQLite (Room) ]
          │
          ├─────────────────────────────────────────┐ (Offline)
          ▼ (Online)                                ▼
   [ Upload ke Cloud Firestore ]           [ Atur Status: "Menunggu Sinkronisasi" ]
          │                                         │
          ├───(Sukses)───> [ Atur Status: "Tersimpan" ]  │ (Koneksi Kembali Aktif)
          │                [ Set isSynced = true ]  │
          │                                         ▼
          └───(Gagal)────> [ Atur Status: "Menunggu Sinkronisasi" ] ──> [ Latar Belakang Sync ]
```

---

## 5. Indikator Visual Status Pada Aplikasi
Indikator status diletakkan di bagian atas Workspace editor, memberikan informasi visual real-time kepada pengguna:

1. **⏳ Menyimpan...** *(Warna Kuning/Gold)*: Sistem sedang memproses penyimpanan ke SQLite lokal atau sedang mentransmisikan data ke Firestore.
2. **🟢 Tersimpan** *(Warna Hijau/Teal)*: Data berhasil tersimpan dengan aman baik di penyimpanan lokal perangkat maupun di server cloud database Firestore.
3. **🔴 Gagal menyimpan** *(Warna Merah/Pink)*: Validasi gagal atau terjadi kesalahan I/O kritis pada database.
4. **🟠 Menunggu sinkronisasi (Offline)** *(Warna Ungu/Orange)*: Data berhasil tersimpan aman secara offline di perangkat lokal SQLite, namun pengunggahan ke database cloud tertunda karena tidak ada koneksi internet. Data akan otomatis terunggah saat internet menyala kembali tanpa perlu menekan tombol apa pun.

---

## 6. Penanganan Konflik & Auto Recovery
Ketika workspace pertama kali dimuat:
1. Aplikasi membaca draf lokal dan meminta draf cloud terbaru secara paralel.
2. Jika terdeteksi perbedaan `timestamp` modifikasi terakhir, pop-up dialog **Auto Recovery & Conflict** akan muncul secara interaktif.
3. Pengguna disuguhkan informasi rinci berupa:
   - Waktu edit versi Lokal vs versi Cloud.
   - Perbandingan panjang karakter kode untuk mencegah kehilangan baris pekerjaan.
4. Memilih **Versi Lokal** akan menerapkan data lokal ke editor dan langsung menyinkronkannya kembali ke Firestore. Memilih **Versi Cloud** akan mengunduh dan menulis ulang data lokal dengan versi cloud terbaru.

---

### Keamanan & Performa Optimal
- **Debounced Writes**: Menghindari pemborosan penulisan penyimpanan flash disk dengan waktu tunggu penundaan 1000ms.
- **Asynchronous Background Processing**: Menggunakan Coroutines `viewModelScope` sehingga I/O penulisan SQLite dan Firestore berjalan pada thread latar belakang (Dispatcher.IO) tanpa menghalangi UI thread (Main Thread), memastikan performa aplikasi tetap lancar dan responsif.
- **Aman Tanpa Internet**: Struktur database mandiri lokal menjamin data tidak akan pernah hilang sekalipun aplikasi ditutup secara paksa oleh OS Android saat offline.
