# SmsFirewall Derleme Dokümantasyonu

Bu doküman, projeyi yerel ortamda adım adım derlemek için hazırlanmıştır.

## Gereksinimler
- Android Studio kurulu olmalıdır (JBR/JDK 21 için).
- Android SDK kurulu olmalıdır.
- `local.properties` dosyasında `sdk.dir` tanımlı olmalıdır.

## Derleme Adımları
1. Proje kök dizinine geçin:

```bash
cd /Users/cansu/AndroidStudioProjects/SmsFirewall
```

2. Varsayılan Java sürümünüz 25 ise, derlemeyi Android Studio JDK 21 ile çalıştırın:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH" \
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug
```

3. Derleme çıktısını doğrulayın:
- Başarılı derlemede terminalde `BUILD SUCCESSFUL` görünür.
- Üretilen debug APK yolu: `app/build/outputs/apk/debug/`

## Sorun Giderme
- `./gradlew` dosyası CRLF satır sonu içeriyorsa macOS/Linux ortamında doğrudan çalışmayabilir.
- Bu durumda dosya satır sonlarını `LF` formatına çevirin veya yukarıdaki `GradleWrapperMain` komutunu kullanın.
