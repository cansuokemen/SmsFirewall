# SmsFirewall - Claude Calisma Notu

Bu dosya Claude veya baska bir kod ajani icin proje baglami, yapilan guvenlik odakli degisiklikler, dogrulama komutlari ve sonraki isler hakkinda kapsamli bir rehberdir.

Son guncelleme: 2026-05-12
Proje yolu: `C:\Users\musta\Desktop\PROJELER\FIREWALL\SmsFirewall`
Branch: `AIModel`

## 1. Hedef ve Genel Durum

SmsFirewall, Android/Kotlin/Jetpack Compose tabanli bir varsayilan SMS uygulamasidir. Amaci SMS mesajlarini listelemek, gondermek, spam olarak siniflandirmak, engellenen gondericileri yonetmek, spam/cop kutusu akisini saglamak ve bildirimleri kontrol etmektir.

Bu calisma turunda oncelik guvenlik ve gizlilikti. Ozellikle su riskler ele alindi:

- SMS iceriginin ve spam/cop verisinin diskte plaintext kalmasi.
- PIN kilidinin tek turlu SHA-256 ile saklanmasi.
- Hassas SharedPreferences degerlerinin plaintext tutulmasi.
- Android Auto Backup ile hassas verinin yedeklenebilmesi.
- Manifest yuzeyinde gereksiz izin ve eksik receiver dogrulamasi.
- Lint'i kiran Compose kaynak okuma hatalari.
- TensorFlow Lite bagimlilik uyumlulugu ve version catalog duzeni.

Mevcut dogrulama sonucu:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
```

Iki komut da basariyla gecti. `connectedAndroidTest` calistirilmadi; cihaz veya emulator gerektirir.

## 2. Cok Onemli Calisma Kurallari

Bu repoda calisirken asagidaki kurallara uy:

- Calisma agaci zaten kirli olabilir. Kullaniciya ait degisiklikleri geri alma.
- `git reset --hard`, `git checkout --`, toplu revert veya temizleme komutlari kullanma.
- Editsiz inceleme icin `rg`, `git diff`, `Get-Content`, `gradlew test/lint` kullan.
- Dosya duzenlemeleri icin patch mantigiyla kucuk ve izlenebilir degisiklikler yap.
- Hassas veriyle ilgili degisikliklerde test ve lint'i mutlaka calistir.
- Android izinleri, varsayilan SMS uygulamasi davranisi ve Play policy hassas oldugu icin manifest degisikliklerini ozellikle dikkatli yap.
- Bu proje Windows/PowerShell ortaminda calisiyor.

## 3. Teknoloji Yigini

- Dil: Kotlin
- UI: Jetpack Compose, Material 3
- DI: Hilt
- Veritabani: Room
- ML: TensorFlow Lite spam siniflandirici
- Android API:
  - `compileSdk` 36
  - `targetSdk` 36
  - `minSdk` 26
- Test:
  - JUnit unit tests
  - AndroidX instrumentation altyapisi
  - Lint aktif ve basari kriteri olarak kullaniliyor

Onemli Gradle dosyalari:

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

## 4. Ana Uygulama Mimarisi

### 4.1 Android Giris Noktalari

- `MainActivity.kt`
  - Varsayilan SMS uygulamasi rolunu kontrol eder.
  - UI'yi baslatir.
  - Tema ve app lock akisini yonetir.
  - Notification permission ister.

- `SmsFirewallApp.kt`
  - Hilt application sinifi.
  - Notification channel olusturur.

- `SmsReceiver.kt`
  - `SMS_DELIVER_ACTION` alir.
  - SMS parcaciklarini birlestirir.
  - `SmsFilterEngine` ile karar verir.
  - Spam ise Room'a kaydeder.
  - Normal SMS ise system inbox'a yazar.
  - Sessiz gonderici ve sessiz saatlere gore bildirim gosterir.

- `MmsReceiver.kt`
  - Varsayilan SMS uygulamasi uygunlugu icin gerekli MMS receiver.
  - Simdilik MMS parsing yapmaz.
  - Yeni durumda action ve MIME type dogrulamasiyla guvenli no-op calisir.

- `SmsFirewallCallScreeningService.kt`
  - Engellenen gonderici listesine gore arama engelleme yapar.

- `RespondViaMessageActivity.kt`
  - Dosya adi yanilticidir; icindeki sinif `RespondViaMessageService`.
  - `android.intent.action.RESPOND_VIA_MESSAGE` action'i icin minimum SMS yanit gonderme akisi eklendi.

### 4.2 Veri Katmani

- `SmsEntity.kt`
  - Room entity.
  - Alanlar: `id`, `sender`, `body`, `receivedAt`, `status`, `reason`.

- `SmsDao.kt`
  - Flow tabanli sorgular.
  - Status bazli silme/sorgulama.
  - Yeni eklenen yardimcilar:
    - `getAllByStatuses(statuses)`
    - `deleteById(id)`
    - `deleteByIds(ids)`

- `SmsRepository.kt`
  - UI ve receiver icin ana veri kapisi.
  - Yeni durumda spam/cop gibi hassas status'ler icin storage encryption burada yapilir.
  - UI tarafina her zaman cozulmus `SmsEntity` doner.
  - Room'a yazarken hassas status'lerde `sender`, `body`, `reason` alanlari `enc:v1:` prefix'li ciphertext olarak saklanir.

### 4.3 Filtreleme

- `SmsFilterEngine.kt`
  - Oncelik sirasi:
    1. Engellenen gonderici.
    2. TFLite spam siniflandirici.
    3. Anahtar kelime filtresi.
    4. Allow.

- `SpamClassifier.kt`
  - `sms_spam_model.tflite`, `sms_spam_vocab.json`, `sms_spam_tflite_config.json` assetlerini kullanir.

- `FilterKeywordStore.kt`
  - Engellenen kelimeleri ve gondericileri tutar.
  - Yeni durumda bu setler SharedPreferences icinde `CryptoBox` ile encrypted string olarak saklanir.
  - Eski plaintext `StringSet` degerleri okununca encrypted formata migrate edilir.

### 4.4 Bildirim ve Tercihler

- `MutedSenderStore.kt`
  - Sessize alinan gondericiler.
  - Yeni durumda encrypted saklama kullanir.

- `NotificationPreferenceStore.kt`
  - Ses, titresim, sessiz saatler.
  - Su an plaintext kaldi; planin kritik ilk fazinda filtre/gonderici/PIN/gizlilik tarafina oncelik verildi.

- `PrivacyPreferenceStore.kt`
  - Cop retention ve private area encryption toggle.
  - Yeni durumda bu degerler encrypted string olarak saklanir.
  - Eski Int/Boolean degerler okununca encrypted formata migrate edilir.

- `AppLockPreferenceStore.kt`
  - Yeni durumda PIN hash PBKDF2WithHmacSHA256 ile uretilir.
  - Salt 16 byte.
  - Iteration: `120_000`.
  - Hash ve salt `CryptoBox` ile encrypted saklanir.
  - Basarisiz deneme sayaci ve 30 saniyelik lockout eklendi.
  - Eski SHA-256 formatla giris yapilabilirse PIN yeni formata migrate edilir.

## 5. Guvenlik Degisiklikleri

### 5.1 CryptoBox

Yeni dosya:

```text
app/src/main/java/com/example/smsfirewall/data/security/CryptoBox.kt
```

Ozellikler:

- Android Keystore kullanir.
- Algoritma: AES/GCM/NoPadding.
- Key alias: `sms_firewall_sensitive_data_v1`.
- Ciphertext prefix: `enc:v1:`.
- IV ciphertext payload icine gomulur.
- `encrypt()` zaten encrypted deger alirsa tekrar encrypt etmez.
- `decrypt()` prefix yoksa plaintext kabul edip aynen dondurur. Bu migration kolayligi icindir.

Bu davranis onemlidir: Mevcut plaintext kayitlar uygulama acilinca veya ilgili preference okununca bozulmadan okunabilir ve yeni formata tasinir.

### 5.2 PIN Hashing

Yeni dosya:

```text
app/src/main/java/com/example/smsfirewall/data/security/PinHasher.kt
```

Ozellikler:

- PBKDF2WithHmacSHA256
- 120.000 iteration
- 256-bit output
- Constant-time verify icin `MessageDigest.isEqual`

Eski SHA-256 format compatibility:

- `AppLockPreferenceStore.verifyPin()` once yeni PBKDF2 formatini dener.
- Gerekirse eski SHA-256 hash'i de dener.
- Eski PIN dogruysa `setPin(pin)` ile yeni formata migrate eder.

### 5.3 Room Spam/Trash Encryption

Uygulama davranisi:

- Spam/cop SMS kayitlari diskte encrypted tutulur.
- UI, filter, restore ve delete akislari plaintext entity gormeye devam eder.
- Repository boundary bu ayrimi saklar.

Hassas status listesi:

```kotlin
listOf(SmsStatus.BLOCK, SmsStatus.TRASH)
```

Encrypted alanlar:

- `sender`
- `body`
- `reason`

Encrypted olmayan alanlar:

- `id`
- `receivedAt`
- `status`

Neden boyle:

- `status` ve `receivedAt` sorgu/silme performansi icin plaintext kaldi.
- Asil hassas PII ve SMS icerigi encrypted.

Migration:

- `InboxViewModel.init` icinde `repository.migratePlaintextSensitiveRows()` cagrilir.
- Mevcut plaintext spam/cop kayitlari encrypted hale getirilir.

### 5.4 Auto Backup

Manifest degisikligi:

```xml
android:allowBackup="false"
```

Kaldirilan dosyalar:

- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

Sebep:

- SMS icerigi ve guvenlik tercihleri hassas veri.
- Privacy-first varsayim geregi cloud/device transfer backup devre disi.

### 5.5 Manifest ve Android Surface Hardening

Yapilanlar:

- `WRITE_SMS` permission kaldirildi.
- `allowBackup=false` yapildi.
- Activity uzerindeki redundant label kaldirildi.
- `MmsReceiver` action ve MIME type kontrolu yapar.
- `RespondViaMessageService` artik sadece ilgili action'da calisir ve minimum SMS yanit akisi uygular.

Dikkat:

- Uygulama varsayilan SMS uygulamasi oldugunda system SMS provider islemleri yapabilir.
- `WRITE_SMS` izninin kaldirilmasi runtime davranisini cihaz bazinda test etmeyi gerektirir.

## 6. Lint ve Kalite Degisiklikleri

Onceki lint sonucu:

- 21 error
- 86 warning
- 3 hint

Yeni durum:

```powershell
.\gradlew.bat lint
```

Basarili.

Temizlenen ana hata grubu:

- Compose icinde `LocalContext.current.getString(...)` ile string okuma.
- Cozum: `stringResource(...)` ile composable scope'ta string degerleri okunup callback icinde o degerler kullanildi.

Diger duzenlemeler:

- `String.format(Locale.ROOT, ...)` kullanildi.
- Compose modifier parameter sirasi lint hatalari temizlendi.
- Gradle dependency literals version catalog'a tasindi.
- TensorFlow Lite `2.14.0` -> `2.17.0`.

Hala derleme warning olarak kalabilen konular:

- Android deprecated system bar color API warningleri.
- Compose `rememberSwipeToDismissBoxState(confirmValueChange=...)` deprecation warning.
- `getParcelableExtra(String)` deprecation warning.

Bunlar lint failure degil; ileride ayrica temizlenebilir.

## 7. Test Durumu

### 7.1 Calisan Komutlar

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat test
.\gradlew.bat lint
```

Hepsi basarili.

### 7.2 Eklenen Unit Test

Yeni dosya:

```text
app/src/test/java/com/example/smsfirewall/SecurityUtilsTest.kt
```

Kapsam:

- PBKDF2 hash dogru PIN'i kabul eder.
- Yanlis PIN'i reddeder.

### 7.3 Eklenen Instrumentation Test

Yeni dosya:

```text
app/src/androidTest/java/com/example/smsfirewall/ManifestSecurityInstrumentedTest.kt
```

Kapsam:

- Backup kapali mi?
- `WRITE_SMS` permission kaldirilmis mi?
- Temel SMS izinleri duruyor mu?
- Allowed SMS notification channel olusuyor mu?

Not:

```powershell
.\gradlew.bat connectedAndroidTest
```

calistirilmadi. Bu komut icin aktif Android cihaz veya emulator gerekir.

## 8. Degisen veya Eklenen Dosyalar

Guvenlik:

- `app/src/main/java/com/example/smsfirewall/data/security/CryptoBox.kt`
- `app/src/main/java/com/example/smsfirewall/data/security/PinHasher.kt`
- `app/src/main/java/com/example/smsfirewall/data/AppLockPreferenceStore.kt`
- `app/src/main/java/com/example/smsfirewall/data/PrivacyPreferenceStore.kt`
- `app/src/main/java/com/example/smsfirewall/data/SmsRepository.kt`
- `app/src/main/java/com/example/smsfirewall/filter/FilterKeywordStore.kt`
- `app/src/main/java/com/example/smsfirewall/notifications/MutedSenderStore.kt`

Room/DAO:

- `app/src/main/java/com/example/smsfirewall/data/local/SmsDao.kt`

DI:

- `app/src/main/java/com/example/smsfirewall/di/AppModule.kt`

Manifest ve Android entrypoint:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/smsfirewall/MmsReceiver.kt`
- `app/src/main/java/com/example/smsfirewall/RespondViaMessageActivity.kt`

UI/lint:

- `app/src/main/java/com/example/smsfirewall/ui/inbox/BlockedSmsScreen.kt`
- `app/src/main/java/com/example/smsfirewall/ui/inbox/ConversationListContent.kt`
- `app/src/main/java/com/example/smsfirewall/ui/inbox/MessageComponents.kt`
- `app/src/main/java/com/example/smsfirewall/ui/inbox/NewMessageScreen.kt`
- `app/src/main/java/com/example/smsfirewall/ui/inbox/SettingsScreen.kt`

Gradle:

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

Silinen:

- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

Test:

- `app/src/test/java/com/example/smsfirewall/SecurityUtilsTest.kt`
- `app/src/androidTest/java/com/example/smsfirewall/ManifestSecurityInstrumentedTest.kt`

## 9. Mevcut Kirli Worktree Hakkinda

Bu dosya yazilirken `git status --short --branch` cok sayida modified ve untracked dosya gosteriyordu. Bunlarin bir kismi bu calisma turunda yapilan degisiklikler, bir kismi ise daha once kullanici tarafindan veya onceki ajan tarafindan eklenmis olabilir.

Ozellikle su dosyalar bu turdan once de untracked gorunuyordu:

- `app/src/main/java/com/example/smsfirewall/data/AppLockPreferenceStore.kt`
- `app/src/main/java/com/example/smsfirewall/data/AppearancePreferenceStore.kt`
- `app/src/main/java/com/example/smsfirewall/data/PrivacyPreferenceStore.kt`
- `app/src/main/java/com/example/smsfirewall/ui/inbox/AppLockScreen.kt`

Bu nedenle Claude:

- Bu dosyalari "ben yaratmadim, silebilirim" diye dusunmemeli.
- Kullanici degisikliklerini korumali.
- Gerekirse `git diff -- <file>` ile net inceleme yapmali.

## 10. Bilinen Sinirlar ve Teknik Borclar

### 10.1 CryptoBox Test Edilebilirligi

`CryptoBox` Android Keystore kullandigi icin JVM unit testte dogrudan kolay test edilemez. Su an unit test `PinHasher` kapsiyor. Daha iyi test icin ileride su yaklasim onerilir:

- `TextCipher` interface ekle.
- Android implementation: `CryptoBox`.
- JVM fake implementation: deterministic base64 veya reversible fake cipher.
- `SmsRepository` encryption/decryption migration testleri fake cipher ile yazilsin.

### 10.2 Room Encryption Granularity

Su an sadece `BLOCK` ve `TRASH` status'leri encrypted. Normal mesajlar system SMS provider'da durdugu icin Room'a alinmiyor. Ancak ileride normal mesajlar Room'a tasinirsa `ALLOW` veya tum local copy alanlari icin encryption stratejisi genisletilmeli.

### 10.3 NotificationPreferenceStore Plaintext

Bildirim sesi URI, quiet hours ve ses/titresim tercihleri su an encrypted degil. Bunlar SMS icerigi veya PIN kadar hassas degil, fakat privacy-hardening icin ileride `ProtectedPreferenceStore` abstraction altina alinabilir.

### 10.4 App Lock Sadece UI Gate

PIN/biometric app lock UI erisimini kisitlar. Tam veri korumasi icin:

- Room encryption zaten eklendi.
- Fakat sistem SMS provider icindeki normal SMS'ler Android sistem tarafinda durur.
- App lock, sistem provider verisini sifreleyemez.

### 10.5 Respond Via Message Davranisi

Minimum yanit akisi eklendi. Ancak cihaz/vendor farklari nedeniyle emulator veya fiziksel cihazda su test edilmeli:

- Arama ekranindan "respond via SMS".
- `sms:` / `smsto:` URI parse.
- Message extra alanlari.
- Permission ve default SMS role davranisi.

### 10.6 TFLite Upgrade

TFLite `2.17.0` olarak guncellendi. Lint page alignment uyarisini gidermek icin yapildi. Model inference davranisi fiziksel cihazda smoke test edilmeli.

## 11. Claude Icin Onerilen Sonraki Gorevler

Oncelik sirasiyla:

1. Emulator veya cihazda smoke test:
   - Uygulama aciliyor mu?
   - Varsayilan SMS role request calisiyor mu?
   - PIN set/verify/lockout calisiyor mu?
   - Spam SMS Room'a encrypted yaziliyor mu?
   - Spam degil restore akisi calisiyor mu?
   - Engellenen gonderici ekle/kaldir akisi calisiyor mu?

2. `connectedAndroidTest` calistir:

   ```powershell
   .\gradlew.bat connectedAndroidTest
   ```

3. Repository encryption icin fake cipher tabanli JVM test altyapisi ekle.

4. Eski plaintext SharedPreferences migration'lari icin unit veya Robolectric test yaz.

5. UI tarafinda kalan deprecation warningleri temizle:
   - System bar APIs.
   - `getParcelableExtra`.
   - SwipeToDismiss confirmValueChange.

6. `NotificationPreferenceStore` ve diger preference store'lari tek bir encrypted preference helper altinda toparla.

7. `RespondViaMessageService` icin daha kapsamli cihaz testi ve hata telemetrisi ekle.

8. `Private area encryption` toggle'inin UX anlamini netlestir:
   - Su an hassas veri encryption default guvenlik davranisi olarak aktif.
   - Toggle sadece preference olarak duruyor.
   - Kullaniciya yanlis guvenlik modeli gostermemek icin metin veya davranis revize edilmeli.

## 12. Kabul Kriterleri

Claude bir sonraki turda degisiklik yaparsa su kriterleri korumali:

- `.\gradlew.bat test` basarili.
- `.\gradlew.bat lint` basarili.
- `allowBackup=false` korunuyor.
- `WRITE_SMS` geri eklenmiyor; ancak net gerekce ve cihaz testi varsa tartisilabilir.
- Spam/cop Room verisi plaintext yazilmiyor.
- PIN tek turlu SHA-256'a geri donmuyor.
- `CryptoBox.PREFIX` uyumlulugu bozulmuyor.
- Eski plaintext kayitlari okuyabilme/migrate edebilme korunuyor.
- Kullaniciya ait mevcut degisiklikler revert edilmiyor.

## 13. Hata Ayiklama Ipuclari

### 13.1 Lint Tekrar Kirmizi Olursa

Rapor:

```powershell
Get-Content -Raw app\build\intermediates\lint_intermediate_text_report\debug\lintReportDebug\lint-results-debug.txt
```

Hizli filtre:

```powershell
rg -n "Error:|Warning:|Hint:" app\build\intermediates\lint_intermediate_text_report\debug\lintReportDebug\lint-results-debug.txt
```

### 13.2 Hilt Compile Hatasi Olursa

Kontrol et:

- Constructor parametreleri Hilt tarafindan provide ediliyor mu?
- `AppModule.kt` icinde provider var mi?
- Singleton state thread-safe mi?

### 13.3 Room Compile Hatasi Olursa

Kontrol et:

- DAO query imzalari Room tarafindan destekleniyor mu?
- `List<String>` parametreli `IN (:statuses)` kullanimi dogru mu?
- Entity alanlari degisti mi? Degistiyse migration gerekir.

### 13.4 Crypto Decrypt Bos Donerse

`CryptoBox.decrypt()` hata halinde bos string dondurur. Bu bilincli olarak crash yerine veri bozulmasi etkisini sinirlamak icin yapildi. Ancak debug icin:

- Keystore key mevcut mu?
- Device restore/backup sonucu key kaybi oldu mu?
- `allowBackup=false` korunuyor mu?
- Ciphertext prefix `enc:v1:` mi?

## 14. Komut Referansi

Derleme:

```powershell
.\gradlew.bat compileDebugKotlin
```

Unit test:

```powershell
.\gradlew.bat test
```

Lint:

```powershell
.\gradlew.bat lint
```

Android instrumentation:

```powershell
.\gradlew.bat connectedAndroidTest
```

Durum:

```powershell
git status --short --branch
git diff --stat
```

Arama:

```powershell
rg -n "CryptoBox|PinHasher|allowBackup|WRITE_SMS|context.getString"
```

## 15. Claude'a Verilebilecek Kisa Prompt

Asagidaki prompt, bu dosyayla birlikte Claude'a verilebilir:

```text
Bu projede SmsFirewall Android uygulamasi uzerinde calisiyorsun. Once CLAUDE.md dosyasini oku. Mevcut guvenlik refaktorunu koru: Keystore CryptoBox, PBKDF2 PIN, encrypted spam/trash Room storage, encrypted blocked/muted/privacy preferences, allowBackup=false ve lint passing state. Kullanici degisikliklerini revert etme. Degisiklik yaptiktan sonra .\gradlew.bat test ve .\gradlew.bat lint calistir. Eger emulator varsa connectedAndroidTest de calistir.
```

## 16. Son Not

Bu proje SMS, rehber, bildirim, cagri tarama ve cihaz role API'leri gibi hassas Android yuzeyleri kullaniyor. Kod degisikligi yaparken "derlendi" yeterli kabul edilmemeli; role, permission ve cihaz davranisi mutlaka pratikte test edilmeli.
