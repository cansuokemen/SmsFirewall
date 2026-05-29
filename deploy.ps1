$JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_HOME = $JAVA_HOME
$env:PATH = "$JAVA_HOME\bin;$env:PATH"

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "`n[1/3] Derleniyor..." -ForegroundColor Cyan
& "$PSScriptRoot\gradlew.bat" ":app:assembleDebug"
if ($LASTEXITCODE -ne 0) { Write-Host "Derleme basarisiz!" -ForegroundColor Red; exit 1 }

$device = (& $adb devices | Select-String -Pattern "^\w+\s+device$" | Where-Object { $_ -notmatch "emulator" } | Select-Object -First 1) -replace "\s+device", ""
if (-not $device) { Write-Host "Telefon bulunamadi! USB bagli ve hata ayiklama acik mi?" -ForegroundColor Red; exit 1 }

Write-Host "`n[2/3] Telefona yukleniyor ($device)..." -ForegroundColor Cyan
& $adb -s $device install -r $apk
if ($LASTEXITCODE -ne 0) { Write-Host "Yukleme basarisiz!" -ForegroundColor Red; exit 1 }

Write-Host "`n[3/3] Uygulama baslatiliyor..." -ForegroundColor Cyan
& $adb -s $device shell am start -n "com.example.smsfirewall/.MainActivity"

Write-Host "`nTamamlandi! Uygulama telefonunda acildi." -ForegroundColor Green
