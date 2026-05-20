# SmsFirewall

Akıllı SMS güvenlik duvarı — spam tespiti, gönderici engelleme, uçtan uca şifreleme ve modern mesajlaşma deneyimi.

---

## Özellikler

### Güvenlik & Gizlilik
- **TensorFlow Lite spam tespiti** — ML modeliyle gelen SMS'ler otomatik sınıflandırılır
- **Anahtar kelime filtresi** — özel kelime ve gönderici engelleme listesi
- **AES/GCM şifreleme** — spam/çöp kutusu kayıtları Android Keystore ile diskte şifreli tutulur
- **PBKDF2 PIN kilidi** — 120.000 iterasyonlu güvenli hash, başarısız deneme sayacı ve 30 saniyelik kilit
- **Biyometrik kilit açma** — parmak izi / yüz tanıma desteği
- **Yedekleme engeli** — `allowBackup=false`, hassas SMS verisi buluta yüklenmiyor

### Mesajlaşma
- **WhatsApp tarzı sohbet arayüzü** — animasyonlu mesaj balonları, giden/gelen ayrımı
- **SMS gönderme** — sistem SMS sağlayıcısı üzerinden doğrudan gönderim
- **Sohbet içi arama** — mesaj ve kelime bazlı anlık filtreleme
- **Tıklanabilir linkler** — mesaj içindeki URL'ler tarayıcıda açılır
- **Bireysel mesaj yıldızlama** — önemli mesajları işaretleme
- **Çoklu seçim** — toplu silme ve işlem desteği

### Bildirimler
- **Bildirimden direkt sohbete** — bildirime tıklayınca ilgili konuşma açılır
- **Sessiz saatler** — belirlenen saatlerde bildirim susturma
- **Gönderici bazlı susturma** — belirli kişilerden bildirim almama
- **Özel bildirim sesi** — sistem dışı ses seçme desteği

### Kişiselleştirme
- **Açık / Koyu / Sistem teması**
- **Dinamik arka plan** — sohbet ekranı için özelleştirilebilir arka plan
- **Mesaj balonu stilleri**
- **Metin boyutu ayarı**

---

## Ekran Görüntüleri

> *(Yakında eklenecek)*

---

## Teknoloji Yığını

| Alan | Teknoloji |
|------|-----------|
| Dil | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Veritabanı | Room (versiyon 3) |
| ML | TensorFlow Lite 2.17.0 |
| Şifreleme | AES/GCM NoPadding (Android Keystore) |
| PIN hash | PBKDF2WithHmacSHA256 |
| Test | JUnit, AndroidX Instrumentation, Lint |

---

## Gereksinimler

- Android **8.0 (API 26)** ve üzeri
- Varsayılan SMS uygulaması izni

---

## Kurulum

### Kaynak koddan derleme

```bash
# Repoyu klonla
git clone https://github.com/cansuokemen/SmsFirewall.git
cd SmsFirewall
git checkout AIModel

# Debug APK oluştur
./gradlew assembleDebug

# Bağlı cihaza yükle
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Windows kullanıcıları `./gradlew` yerine `.\gradlew.bat` kullanmalıdır.

### Gerekli izinler

Uygulama ilk açılışta aşağıdaki izinleri ister:

| İzin | Amaç |
|------|------|
| `RECEIVE_SMS` | Gelen SMS'leri alma |
| `READ_SMS` | SMS geçmişini okuma |
| `SEND_SMS` | SMS gönderme |
| `READ_CONTACTS` | Kişi adlarını gösterme |
| `POST_NOTIFICATIONS` | Bildirim gösterme (Android 13+) |
| `USE_BIOMETRIC` | Biyometrik kilit açma |

---

## Mimari

```
SmsFirewall/
├── data/
│   ├── local/          # Room entity, DAO, AppDatabase
│   ├── security/       # CryptoBox (AES/GCM), PinHasher (PBKDF2)
│   └── SmsRepository   # Şifreleme/çözme sınırı, UI'ya plaintext döner
├── filter/
│   ├── SmsFilterEngine # Karar zinciri: gönderici → ML → anahtar kelime
│   └── SpamClassifier  # TFLite çalıştırıcı
├── notifications/      # Bildirim kanalı, tercihler, sessiz gönderici
├── ui/
│   ├── inbox/          # Ana ekranlar ve ViewModel
│   └── theme/          # Renk, tipografi, tema
└── di/                 # Hilt modülleri
```

### Veri akışı

```
Gelen SMS
    └─► SmsReceiver
            ├─► SmsFilterEngine ──► BLOCK → Room (şifreli)
            └─► ALLOW → System Inbox + Bildirim
```

### Şifreleme stratejisi

- Spam ve çöp kutusu kayıtlarının `sender`, `body`, `reason` alanları `enc:v1:<base64>` formatında saklanır.
- Android Keystore anahtarı cihaz dışına çıkmaz; yedekleme devre dışıdır.
- Eski plaintext kayıtlar ilk açılışta otomatik olarak şifreli formata taşınır.

---

## Test

```bash
# Unit testler
./gradlew test

# Lint
./gradlew lint

# Cihaz/emülatör testleri
./gradlew connectedAndroidTest
```

---

## Lisans

Bu proje özel kullanım amaçlıdır. Lisans bilgisi için repo sahibiyle iletişime geçin.
