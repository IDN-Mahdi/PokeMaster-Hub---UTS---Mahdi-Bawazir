# PokéMaster Hub: National 1025 & HGSS Tactical Suite

**PokéMaster Hub** adalah aplikasi ensiklopedia kanonikal dan asisten simulasi taktis Pokémon berskala penuh (**National Dex 1.025**, **Johto HGSS 256**, **Kalos 457**, & **Hisui 242**) yang dikembangkan dengan antarmuka **Luxury Minimalist Dark Slate** yang responsif, mengintegrasikan data dinamis dari **REST API PokéAPI v2**, serta memiliki arsitektur penyimpanan lokal berlapis (*Tri-Layer Offline-First Persistence*).

Proyek ini dirancang sebagai satu aplikasi terpadu untuk memenuhi seluruh kriteria evaluasi **Ujian Tengah Semester (UTS)** pada 3 mata kuliah sekaligus:
1. **UTS Pemrograman Mobile (Teori)**
2. **UTS Pemrograman Multiplatform**
3. **UTS Praktikum Pemrograman Mobile**

---

## 👨‍💻 Identitas Pengembang
* **Nama:** Mahdi Bawazir
* **NIM:** 240605110021
* **Program Studi:** Teknik Informatika
* **Universitas:** UIN Maulana Malik Ibrahim Malang
* **Repositori GitHub:** [https://github.com/IDN-Mahdi/PokeMaster-Hub---UTS---Mahdi-Bawazir](https://github.com/IDN-Mahdi/PokeMaster-Hub---UTS---Mahdi-Bawazir)

---

## ✅ Matriks Pemenuhan Kriteria Wajib UTS

| Kriteria Evaluasi UTS | Implementasi pada Proyek | Status |
| :--- | :--- | :---: |
| **1. Struktur GUI & Navigasi Multi-Rute** | 5 Halaman Utama pada `BottomNavigationBar` (**Pokédex**, **Johto HGSS**, **Party Lab**, **Matchup**, **Trainer & Cup**) + Halaman/Modal Detail Spesies mandiri (`Navigator.push` / Named Routes). | **Terpenuhi 100%** |
| **2. Integrasi REST API & Model Class** | Pengambilan data langsung via `HTTP GET` ke endpoint resmi `https://pokeapi.co/api/v2/pokemon/{id}` dan `pokemon-species/{id}` yang dideserialisasi menggunakan `factory PokemonModel.fromJson()`. | **Terpenuhi 100%** |
| **3. Asynchronous Programming & `FutureBuilder`** | Penerapan `Future`, `async`/`await`, serta `FutureBuilder` yang menangani 3 kondisi secara eksplisit: **Loading** (`CircularProgressIndicator`), **Error** (pesan kesalahan + tombol *Coba Lagi*), dan **Success** (render data & *offline cache*). | **Terpenuhi 100%** |
| **4. Data Persistence (3 dari 3 Teknik)** | Menerapkan **ketiga teknik sekaligus**: **SharedPreferences** (konfigurasi & kuota), **SQLite / `sqflite`** (5 tabel relasional), dan **Read/Write Files** (`path_provider` & `dart:io` untuk `pokemaster_autosave.json` & cache API). | **Terpenuhi 100%** |
| **5. Error Handling & Lifecycle Safety** | Seluruh proses I/O dan jaringan dibungkus blok `try-catch`, dilengkapi penjaga `if (!mounted) return;` sebelum pembaruan state antarmuka (`setState` / `StateFlow`). | **Terpenuhi 100%** |

---

## ✨ Fitur Utama Aplikasi

### 1. Dynamic Responsive Pokédex (1–5 Kolom) & Canonical 1.025 Database
* Menampilkan katalog **1.025 spesies Pokémon** dengan data kanonikal resmi 100% akurat (Tipe Primer & Sekunder, 6 *Base Stats* HP/Atk/Def/SpA/SpD/Spe, *Base Stat Total*, dan 540 rantai keluarga evolusi).
* **Mesin Auto-Scaling Grid `[1 | 2 | 3 | 4 | 5]`:** Mengadaptasi tata letak kartu secara dinamis dari *Spacious Editorial View* (1–2 kolom), *Balanced Compact View* (3 kolom), hingga *High-Density Micro-Grid* (4–5 kolom dengan kode tipe 3 huruf resmi seperti `GRS`, `PSN`, `FIR`, `DRG`) tanpa tabrakan atau pemotongan teks vertikal.
* **Sistem Gestur Kartu Tanpa Panel Dalam (*Seamless Card*):**
  * **Ketuk 1/3 Atas Kartu (Mode Picker):** Kiri = *Like: Design* (Putih Mutiara `#F1F5F9`), Tengah = *Like: Icon* (Emas `#F59E0B`), Kanan = *Like: Party* (Merah Ruby `#E11D48`) dengan pembagian warna horizontal otomatis (**Merah → Emas → Putih**) dan kontras teks adaptif (*Adaptive Contrast Ink*).
  * **Ketuk Setengah Bawah Kartu:** 1× Ketuk = *Dislike* (`#1E1B4B`), 2× Ketuk = *Reset/Undo*.
  * **Tekan & Tahan (*Hold / Long-Press*):** Membuka **Modal Detail Spesies** yang memuat toggle *Prefer Shiny* per-Pokémon, tombol *+ Add to Party Lab*, rincian 6 *Base Stats*, pemutar suara (*Audio Cry*), dan sinkronisasi langsung PokéAPI v2.

### 2. Auto-Sync Generasi & 22 Spesies Pre-Fairy
* Dilengkapi pemilih generasi tipe (**Gen 1 [15T]**, **Gen 2-5 [17T]**, **Gen 6-9 [18T]**).
* Saat pengguna beralih ke **Gen 1 (15T)** atau **Gen 2-5 (17T)**, aplikasi secara otomatis mengonversi tipe **22 Pokémon Pre-Fairy** (seperti *Clefable*, *Togekiss*, *Gardevoir*, *Marill*, *Azumarill*, *Mawile*, *Cottonee*, *Whimsicott*) ke tipe asli pra-Gen 6 mereka, serta mengubah *Magnemite* & *Magneton* menjadi *Pure Electric* pada **Gen 1 (15T)**.

### 3. Detail Spesies Berbasis Live REST API & Offline Cache
* Mengekstrak data dinamis (dimensi tinggi/berat, *base experience*, *abilities*, daftar *moves*, dan deskripsi resmi Pokédex) secara asinkron dari **PokéAPI v2**.
* Dilengkapi mekanisme penulisan *cache* JSON ke penyimpanan berkas lokal (`pokemon_cache_$id.json`) sehingga detail spesies tetap dapat diakses secara instan meskipun perangkat sedang luring (*offline*).

### 4. 6-Slot Tactical Party Lab
Laboratorium taktis untuk merakit 6 anggota tim tempur lengkap dengan mode tampilan **Full Cards** maupun **Compact 2×3 Grid**, toggle **Normal / ✦ Shiny** per slot, konfigurasi **4 Move (`Physical` / `Special`)**, serta baris ke-5 **✦ Secret Type (*Hidden Power / Tera*)**:
* **Panel A — "How Your Team Takes Damage" (*Tiered Threat & Shield Radar*):**
  * Menampilkan skor pertahanan tim (*Defense Score Grade S–D*).
  * **Zone 1 (Danger Zones & Threat Radar):** Mengurutkan ancaman terbesar lengkap dengan mini-sprite anggota tim yang terkena kelemahan `4×` (merah) atau `2×` (oranye) serta indikator status **`✓ Shield`** vs **`⚠ Unshielded`**.
  * **Zone 2 (Team Shields & Immunities):** Merinci tipe serangan lawan yang berhasil ditahan (`½×`, `¼×`) atau dikebalkan (`0×`) beserta mini-sprite Pokémon pelindung.
  * **Zone 3 (Pure Neutral Damage):** Baris ringkas tipe yang menghasilkan kerusakan netral (`1×`).
* **Panel B — "What Your Team Can Hit Hard" (*Tactical Strike & Coverage Matrix*):**
  * Dilengkapi *Bi-Directional Physical vs Special Balance Bar*.
  * **Zone 1 (Uncovered Blind Spots):** Kartu peringatan di bagian atas yang menyoroti tipe lawan yang belum bisa dipukul `2× Super Effective` beserta rekomendasi tipe serangan penambal *blind spot*.
  * **Zone 2 (Super-Effective Coverage Matrix):** Menampilkan kapasitas pukulan tim (`0/6` s/d `6/6` anggota) dengan **6-Segment Mini Power Bar** dan *interactive drill-down* untuk melihat Pokémon serta move mana yang menghantam tipe target tersebut.
* **Interactive AI Tactical Coach:** Asisten strategi interaktif dua arah yang mengaudit celah pertahanan, *blind spots*, dan keseimbangan *Physical/Special* secara *real-time*.

### 5. Matchup Calculator (Split STAB) & Hall of Fame Studio
* **Matchup:** Kalkulator tipe tunggal & ganda (6×3 Grid) dengan bagian pertahanan **"How You Take Damage"** dan bagian serangan **"What You Can Hit Hard"** yang memisahkan proyeksi serangan (**Primary Type STAB Moves** dan **Secondary Type STAB Moves**) secara mandiri agar tidak tercampur, ditambah fitur *Battle Drills Practice* dan *Single-Type Logic Analogies*.
* **Trainer & Cup:** Menampilkan *Trainer Passport*, turnamen eliminasi 64-bracket, serta kanvas animasi kemenangan **"League Champion! Congratulations!"** bergaya *HeartGold/SoulSilver* (HGSS) yang dapat dirender dan dibagikan sebagai gambar **PNG**.

---

## 💾 Tri-Layer Data Persistence (Penyimpanan Lokal)

Aplikasi ini melampaui batas minimal tugas dengan mengimplementasikan **3 teknik penyimpanan data lokal sekaligus**:
1. **SharedPreferences (`shared_preferences`):** Menyimpan konfigurasi antarmuka berkecepatan tinggi seperti mode mekanik generasi (`mechanic_mode`), status *Shiny* global & daftar ID *Prefer Shiny* per-Pokémon, *streak* kuis, serta kuota tipe Johto.
2. **SQLite Relational Database (`sqflite`):** Mengelola basis data relasional `pokemaster_hub.db` yang mencakup tabel `pokemon_ratings`, `pokedex_tracker`, `recent_ratings`, `recent_tracker_actions`, dan `party_roster` (menyimpan 6 slot tim beserta metadata 4 Move, Secret Type, dan Shiny).
3. **Read and Write Files (`path_provider` & `dart:io`):** Menulis seluruh state aplikasi secara otomatis ke berkas cadangan lokal `pokemaster_autosave.json`, mendukung ekspor/impor JSON & format *Pokémon Showdown*, serta menyimpan cache respons PokéAPI v2 ke berkas `pokemon_cache_$id.json`.

---

## 📂 Struktur Direktori Proyek

```text
pokemaster_hub/
├── lib/
│   ├── main.dart                        # Entry point, Named Routes, BottomNavigationBar, & FutureBuilder 3-State
│   ├── models/
│   │   └── pokemon_model.dart           # Model Class dengan factory PokemonModel.fromJson() & toJson()
│   └── services/
│       ├── pokeapi_service.dart         # Klien HTTP GET ke PokéAPI v2 + Offline File JSON Cache
│       └── persistence_service.dart     # Manajer Tri-Layer Persistence (SharedPreferences, SQLite, & File I/O)
├── app/src/main/java/com/example/       # Modul Native Android (Jetpack Compose + Clean MVVM Architecture)
│   ├── data/                            # Canonical 1.025 Repository, SQLite Helper, SharedPreferences, & File Storage
│   ├── ai/                              # TacticalCoachAI & Coverage Matrix Engine
│   └── ui/                              # ViewModel, Responsive PokemonCard (1-5 Cols), & 5 Utama Screens
└── pubspec.yaml                         # Konfigurasi dependensi proyek
```

---

## 🛠️ Teknologi & Package yang Digunakan
* **Flutter SDK:** `>=3.2.0 <4.0.0` (Dart) & **Kotlin / Jetpack Compose** (Material Design 3)
* **`http` (`^1.2.0`) / `okhttp3`:** Klien HTTP untuk sinkronisasi asinkron REST API PokéAPI v2.
* **`shared_preferences` (`^2.2.2`):** Penyimpanan *key-value* lokal untuk preferensi pengguna.
* **`sqflite` (`^2.3.2`) & `path` (`^1.9.0`):** Mesin basis data relasional SQLite lokal.
* **`path_provider` (`^2.1.2`):** Akses direktori dokumen perangkat untuk operasi baca/tulis (*I/O*) berkas JSON lokal.
* **`audioplayers` (`^6.0.0`):** Pemutaran audio *cries* resmi Pokémon dari repositori CDN PokéAPI.

---

## 🚀 Langkah Menjalankan Aplikasi

1. Pastikan lingkungan pengembangan **Flutter SDK** (atau **Android Studio**) telah terpasang dan dikonfigurasi dengan benar di perangkat Anda.
2. *Clone* repositori ini ke komputer Anda:
   ```bash
   git clone https://github.com/IDN-Mahdi/PokeMaster-Hub---UTS---Mahdi-Bawazir.git
   cd PokeMaster-Hub---UTS---Mahdi-Bawazir
   ```
3. Unduh seluruh paket dependensi proyek:
   ```bash
   flutter pub get
   ```
4. Jalankan aplikasi pada emulator atau perangkat fisik yang terhubung:
   ```bash
   flutter run
   ```
5. *(Opsional — Build APK Rilis):*
   ```bash
   flutter build apk --release
   ```

---

© 2026 **Mahdi Bawazir** (240605110021) — Teknik Informatika, UIN Maulana Malik Ibrahim Malang.
