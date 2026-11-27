#!/usr/bin/env bash
# Instala capri.apk (y 1.11.7 → capri.apk encima) en el emulador y dice qué contesta Android.
pkg=io.github.emmanuelquintana.capri
echo "=== Android $(adb shell getprop ro.build.version.release) (API $(adb shell getprop ro.build.version.sdk)), ABIs: $(adb shell getprop ro.product.cpu.abilist)"
adb shell df -h /data | tail -1

echo "=== 1) instalación limpia"
adb install capri.apk 2>&1 | tail -3
adb shell dumpsys package $pkg | grep -E "versionName|primaryCpuAbi|legacyNativeLibraryDir" | head -4
adb uninstall $pkg >/dev/null 2>&1

echo "=== 2) como lo abre el teléfono: copiado a Descargas e instalado con pm (sesión)"
adb push capri.apk /data/local/tmp/capri.apk >/dev/null
adb shell pm install -r /data/local/tmp/capri.apk 2>&1 | tail -3
adb uninstall $pkg >/dev/null 2>&1

echo "=== 3) 1.11.7 y luego la nueva encima"
adb install old.apk 2>&1 | tail -1
adb install -r capri.apk 2>&1 | tail -3
adb shell dumpsys package $pkg | grep -E "versionName" | head -2

echo "=== 4) que abra"
adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 12
adb shell pidof $pkg && echo "abre: sí" || { echo "abre: NO"; adb logcat -d | grep -E "AndroidRuntime|FATAL|UnsatisfiedLink" | tail -20; }
exit 0
