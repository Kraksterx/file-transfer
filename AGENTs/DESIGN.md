# DESIGN.md — Sistem Desain Material 3

**Proyek:** Wi-Fi Direct File Transfer · Jetpack Compose · Material Design 3 **Kepribadian:** tegas, netral, minim dekorasi — hitam sebagai Primary, putih sebagai kanvas

---

## 1. Seed Color & Tonal Palette

**Seed yang direkomendasikan:** `#1A1A1A` dengan chroma sangat rendah (≈4, bukan 0 — chroma nol membuat algoritma HCT M3 kesulitan menghasilkan tangga tone yang halus/sedikit "patah"). Verifikasi presisi lewat [Material Theme Builder](https://m3.material.io/theme-builder) dengan seed ini; nilai di bawah adalah aproksimasi tangga neutral standar untuk referensi cepat tim:

| Tone | Hex (approx) | Pemakaian umum |
| --- | --- | --- |
| 0 | `#000000` | Teks di atas surface sangat terang (jarang dipakai langsung) |
| 10 | `#1A1A1A` | **Primary (light theme)** — CTA utama |
| 20 | `#303030` | PrimaryContainer gelap / Surface tergelap (dark theme) |
| 30 | `#474747` | **Secondary (light theme)** |
| 40 | `#5E5E5E` | Outline kuat, ikon sekunder |
| 50 | `#767676` | Teks tersier, placeholder |
| 60 | `#8F8F8F` | Outline standar |
| 70 | `#A9A9A9` | Divider, border nonaktif |
| 80 | `#C4C4C4` | **Primary (dark theme)** |
| 90 | `#E0E0E0` | SurfaceVariant (light), PrimaryContainer (dark) |
| 95 | `#F0F0F0` | Surface terangkat (light) |
| 99 | `#FCFCFC` | Background (light theme) |
| 100 | `#FFFFFF` | **OnPrimary (light theme)** |

**Kenapa dark theme tidak memakai `#000000` murni sebagai Background:** rekomendasi Material sendiri memakai `#121212` (bukan hitam mutlak) karena (a) shadow/elevation tidak terlihat di atas hitam sempurna — satu-satunya cara membedakan layer jadi hilang, (b) kontras ekstrem putih-di-atas-hitam-mutlak melelahkan mata saat dipakai lama, (c) di layar OLED, hitam mutlak + scroll cepat memicu efek *smearing*. Jadi dark theme kalian: Background `#121212`, Surface sedikit lebih terang (`#1E1E1E`) untuk kartu/dialog supaya elevasi terasa tanpa warna.

---

## 2. Tabel Role Warna (Light / Dark)

| Role | Light | Dark | Dipakai di |
| --- | --- | --- | --- |
| Primary | `#1A1A1A` | `#C4C4C4` | `Button` (Filled), `TopAppBar` container |
| OnPrimary | `#FFFFFF` | `#1A1A1A` | Teks/ikon di atas Primary |
| PrimaryContainer | `#303030` | `#474747` | State terangkat dari Primary (jarang dipakai di app ini) |
| Secondary | `#474747` | `#A9A9A9` | `OutlinedButton`, `AssistChip` netral |
| OnSecondary | `#FFFFFF` | `#1A1A1A` | Teks/ikon di atas Secondary |
| Background | `#FCFCFC` | `#121212` | Kanvas layar |
| OnBackground | `#1A1A1A` | `#E0E0E0` | Teks body di atas kanvas |
| Surface | `#FFFFFF` | `#1E1E1E` | `Card`, dialog, bottom sheet |
| OnSurface | `#1A1A1A` | `#E0E0E0` | Teks di atas Surface |
| SurfaceVariant | `#E0E0E0` | `#2C2C2C` | Latar item lampiran, input field |
| Outline | `#8F8F8F` | `#8F8F8F` | Border `OutlinedButton`, `OutlinedTextField` |

**Warna semantik tetap dipertahankan (TIDAK dimonokromkan):** status transfer perlu dibedakan sekilas tanpa membaca teks — ini langsung relevan ke `AssistChip` status "Terkirim/Diterima/Ditolak/Gagal" di wireframe Riwayat kita.

| Peran | Light | Dark | Dipakai di |
| --- | --- | --- | --- |
| Success | `#2E7D32` | `#81C995` | Chip "Terkirim"/"Diterima" |
| Error | `#B3261E` | `#F2B8B5` | Chip "Gagal", banner prasyarat |
| Warning | `#8F5000` | `#FFB868` | Chip "Diundang...", banner Mode Lokasi mati |

---

## 3. Tipografi

Asumsi: belum ada font brand khusus, jadi direkomendasikan **Roboto** (default M3, netral, nol dependency tambahan) — cocok dengan kepribadian "tegas, minim dekorasi". Kalau tim ingin identitas lebih personal, **Inter** adalah alternatif drop-in terdekat (metrik serupa, lebih geometris).

| Style | Size / Line height | Weight | Dipakai di |
| --- | --- | --- | --- |
| Headline Small | 24 / 32 | Medium (500) | Judul `TopAppBar` layar utama |
| Title Large | 22 / 28 | Medium (500) | Judul dialog ("Permintaan Transfer Masuk") |
| Title Medium | 16 / 24 | Medium (500) | Nama peer di `Card`, judul kartu riwayat |
| Body Large | 16 / 24 | Regular (400) | Isi pesan, deskripsi banner |
| Body Medium | 14 / 20 | Regular (400) | Subjudul kartu, ukuran file |
| Label Large | 14 / 20 | Medium (500) | Teks tombol (`Button`, `OutlinedButton`) |
| Label Small | 11 / 16 | Medium (500) | Status chip ("Tersedia", "Terhubung") |

---

## 4. Theme.kt (kerangka)

```kotlin
// ui/theme/Color.kt
val md_primary_light = Color(0xFF1A1A1A)
val md_onPrimary_light = Color(0xFFFFFFFF)
val md_secondary_light = Color(0xFF474747)
val md_background_light = Color(0xFFFCFCFC)
val md_surface_light = Color(0xFFFFFFFF)
val md_outline_light = Color(0xFF8F8F8F)

val md_primary_dark = Color(0xFFC4C4C4)
val md_onPrimary_dark = Color(0xFF1A1A1A)
val md_secondary_dark = Color(0xFFA9A9A9)
val md_background_dark = Color(0xFF121212)
val md_surface_dark = Color(0xFF1E1E1E)
val md_outline_dark = Color(0xFF8F8F8F)

val md_success = Color(0xFF2E7D32)
val md_error = Color(0xFFB3261E)
val md_warning = Color(0xFF8F5000)

// ui/theme/Theme.kt
@Composable
fun P2pFileTransferTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = md_primary_dark, onPrimary = md_onPrimary_dark,
            secondary = md_secondary_dark, background = md_background_dark,
            surface = md_surface_dark, outline = md_outline_dark
        )
    } else {
        lightColorScheme(
            primary = md_primary_light, onPrimary = md_onPrimary_light,
            secondary = md_secondary_light, background = md_background_light,
            surface = md_surface_light, outline = md_outline_light
        )
    }
    MaterialTheme(colorScheme = colorScheme, typography = P2pTypography, content = content)
}
```

Catatan: `success`/`error`/`warning` **bukan** bagian dari `ColorScheme` bawaan M3 — buat `object SemanticColors` terpisah atau `CompositionLocal` kustom, lalu akses lewat `SemanticColors.success` di komponen chip.

---

## 5. Elevasi & State Layer tanpa warna

Karena palet monokrom, elevasi tidak bisa mengandalkan tint warna seperti M3 default (yang biasanya mencampur Primary ke Surface). Gantinya:

- **Light theme:** `Card` memakai `tonalElevation` bawaan (Surface → sedikit lebih terang) + shadow tipis.
- **Dark theme:** `Card` memakai variasi Surface (`#1E1E1E` vs Background `#121212`) — beda tone, bukan beda warna.
- **State layer** (hover/pressed/focus): overlay hitam 8% (light theme) atau putih 8% (dark theme) di atas komponen — standar M3 opacity 8%/12%/16% untuk hover/focus/pressed.

---

## 6. Pemetaan Komponen (merujuk wireframe & legenda M3 sebelumnya)

| Komponen (no. wireframe) | Token warna |
| --- | --- |
| `Button` Filled \[3\]\[6\]\[22\] | Primary / OnPrimary |
| `OutlinedButton` \[7\]\[19\]\[21\] | Outline (border) + OnSurface (teks) |
| `TextButton` \[5\]\[15\] | OnSurface, tanpa background |
| `AssistChip` status \[2\]\[28\] | Success/Error/Warning — **bukan** Primary/Secondary |
| `Card` \[4\]\[17\]\[27\] | Surface / OnSurface |
| `TopAppBar` \[1\]\[16\] | Surface / OnSurface (bukan Primary — supaya hierarki CTA di body tidak kalah menonjol) |
| `NavigationBar` \[8\] | Surface, indikator aktif pakai Secondary |

---

## 7. Checklist Aksesibilitas

- Primary `#1A1A1A` di atas OnPrimary `#FFFFFF`: rasio kontras ≈ 16:1 — jauh melebihi WCAG AA (4.5:1).
- Secondary `#474747` di atas putih: ≈ 8.3:1 — aman.
- Pastikan Success/Error/Warning tetap lolos 4.5:1 terhadap Surface masing-masing tema — nilai di §2 sudah dipilih dengan itu (perlu verifikasi ulang begitu font & ukuran final dipakai).

---

## 8. Belum Diputuskan (Bebberapa Sudah)

1. Font final: Poppins.
2. Apakah memakai Dynamic Color Android 12+ (`dynamicColorScheme`) sebagai opsi tambahan, atau palet statis di atas saja. Putusan kami: statis aja
3. Nilai chroma seed (`#1A1A1A`, chroma ≈4) perlu diverifikasi lewat Material Theme Builder untuk tangga tone yang presisi — tabel §1 adalah aproksimasi.