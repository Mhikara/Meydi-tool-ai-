# MeydiAi - Hybrid Structure (React Native Expo & Node.js Backend)

Project ini berisi arsitektur dasar untuk diimplementasikan menggunakan **Expo (React Native)** untuk Mobile App (Frontend) dan **Node.js Express dengan Remotion/FFmpeg** untuk Rendering Engine (Backend).

## 📂 Struktur Folder Proyek

```text
meydiai-project/
├── mobile-expo/                # Frontend: React Native + Expo App
│   ├── assets/                 # Aset lokal (gambar, ikon, dll.)
│   ├── components/             # Komponen UI Modular
│   │   ├── CodeEditor.js       # Editor kode (WebView atau react-native-code-editor)
│   │   ├── CanvasPreview.js    # Kotak live preview berbasis WebView
│   │   └── PromptInput.js      # Input area generator teks ke kode AI
│   ├── screens/                # Layar Utama Aplikasi
│   │   ├── HomeScreen.js       # Dashboard utama
│   │   ├── WorkspaceScreen.js  # Studio Editor & Canvas Builder
│   │   └── LibraryScreen.js    # Menyimpan daftar prompt template siap pakai
│   ├── services/               # Integrasi API (Gemini & Render Service)
│   │   ├── api.js              # Fetch connection ke backend render (simulasi)
│   │   └── gemini.js           # API call direct ke model Gemini AI
│   ├── App.js                  # Entry point aplikasi Expo
│   ├── package.json            # Daftar dependensi Expo (Zundand, React Native Paper, dll.)
│   └── app.json                # Konfigurasi project Expo
│
└── server-backend/             # Backend: Node.js, Express, FFmpeg & Remotion Lambda
    ├── src/
    │   ├── templates/          # Template Remotion / Canvas boilerplate
    │   │   └── CanvasVideo.tsx # Template Remotion untuk membungkus Canvas Javascript
    │   ├── render/             # Logika eksekusi FFmpeg & Remotion SSR
    │   │   └── renderEngine.js # Proses render frame-by-frame ke file MP4
    │   └── server.js           # Express API Router & Endpoint
    ├── package.json            # Dependensi Backend (express, remotion, @remotion/renderer, fluent-ffmpeg)
    └── Dockerfile              # Docker konfig untuk deploy ke cloud (dilengkapi FFmpeg bin)
```

## 🚀 Panduan Instalasi & Menjalankan Aplikasi

### 1. Inisialisasi Backend (Node.js & Express)
1. Pindah ke direktori backend:
   ```bash
   cd server-backend
   ```
2. Pasang semua dependensi:
   ```bash
   npm install
   ```
3. Pastikan mesin Anda memiliki **FFmpeg** terpasang (`sudo apt install ffmpeg` di Linux atau `brew install ffmpeg` di macOS).
4. Jalankan server lokal:
   ```bash
   npm run start
   ```
   Backend akan berjalan di `http://localhost:5000` dengan endpoint utama:
   - `POST /api/generate-code` - Meneruskan prompt ke AI (Gemini) dan mengembalikan kode JS Canvas.
   - `POST /api/render` - Menerima kode JS Canvas dan memprosesnya menggunakan headless-browser (Puppeteer) bersama FFmpeg menjadi file MP4 berdurasi looping (misal 5-10 detik) berukuran standar microstock.

### 2. Inisialisasi Frontend (Expo App)
1. Pindah ke direktori mobile-expo:
   ```bash
   cd mobile-expo
   ```
2. Pasang dependensi:
   ```bash
   npm install
   ```
3. Jalankan aplikasi Expo:
   ```bash
   npx expo start
   ```
4. Pindai kode QR menggunakan perangkat Android dengan aplikasi Expo Go untuk melihat preview-nya secara langsung.
