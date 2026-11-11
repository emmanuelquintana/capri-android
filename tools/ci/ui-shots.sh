#!/usr/bin/env bash
# Walks the app on an emulator (CI, ui-shots.yml) and prints small screenshots into the log as
# base64 between markers, so the app's screens can be looked at without a phone.
set -u
PKG=io.github.emmanuelquintana.capri
APK=${1:-app/build/outputs/apk/debug/app-debug.apk}
mkdir -p shots
adb install -r "$APK" >/dev/null
adb shell settings put global window_animation_scale 1
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null
adb shell cmd locale set-app-locales $PKG --locales es-ES # as Emma sees it

# the view tree on screen (uiautomator is flaky on a busy emulator: a few tries)
dump() {
  rm -f /tmp/ui.xml
  for i in 1 2 3 4 5; do
    adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
    grep -q '<hierarchy' /tmp/ui.xml 2>/dev/null && return 0
    sleep 2
  done
  echo "dump failed"
}
# tap the first view whose text contains $1 (case-insensitive)
# tap it, scrolling down the page until it shows (at most $2 swipes)
tapdown() {
  for _ in $(seq 0 "${2:-6}"); do
    dump
    if grep -qi "text=\"$1\"" /tmp/ui.xml; then tap "$1"; return; fi
    adb shell input swipe 500 1700 500 900 300
    sleep 1
  done
  echo "no '$1' on screen (scrolled)"
}
tap() {
  dump
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys
want = sys.argv[1].lower()
xml = open('/tmp/ui.xml', encoding='utf-8').read()
nodes = list(re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml))
# a node that says exactly that first (a tab), else one that has it in its text
exact = [m for m in nodes if want in (m.group(1).lower().strip(), m.group(2).lower().strip())]
some = [m for m in nodes if want in m.group(1).lower() or want in m.group(2).lower()]
for m in (exact or some)[:1]:
    x1, y1, x2, y2 = map(int, m.groups()[2:])
    print((x1 + x2) // 2, (y1 + y2) // 2)
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "tapped '$1' at $xy"; else echo "no '$1' on screen"; fi
}
shot() {
  sleep "${2:-2}"
  # the emulator gone: say so and stop, instead of waiting for it until the job times out
  if ! timeout 20 adb get-state >/dev/null 2>&1; then
    echo "!!! EL EMULADOR SE CAYÓ antes de la captura $1"
    exit 3
  fi
  adb exec-out screencap -p > "shots/$1.png"
  convert "shots/$1.png" -resize 400x "shots/$1.small.png" 2>/dev/null || cp "shots/$1.png" "shots/$1.small.png"
  echo "===SHOT $1==="; base64 -w0 "shots/$1.small.png"; echo; echo "===END $1==="
}

echo "Android $(adb shell getprop ro.build.version.release) · WebView: $(adb shell dumpsys webviewupdate | grep -m1 -o 'Current WebView package.*')"
adb shell am start -n $PKG/.MainActivity >/dev/null
shot 01-boot-early 2
shot 02-boot 7
tap "empezar"
shot 03-name 3
adb shell input text "Emma"
shot 04-name-typed 1
tap "siguiente"
shot 05-ai 3
tap "con una clave"
shot 06-ai-key 2
tap "ollama"
shot 07-ai-pc 2
tap "gratis"
tap "siguiente"
shot 08-perms 5
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
tap "siguiente"
shot 10-done 3
# before she starts (her page animates forever, and the hierarchy dump waits for stillness):
# where the pill bar's tabs are, from the main screen
adb shell appops set $PKG SYSTEM_ALERT_WINDOW deny
tap "iniciar"
shot 11-main 3
# the one-time "join the community" invite, then away
shot 11b-community 1
tap "ahora no"
sleep 1
adb shell input swipe 500 1800 500 600 300
shot 12-main-scrolled 2
# 1.9.0: «Tú» (you), «Personalizar» (her looks) and «Ajustes» (the general, permissions first)
tap "tú"
shot 13-you-tab 2
adb shell input swipe 500 1800 500 700 300
shot 13a-you-more 2
tap "personalizar"
shot 13b-style-tab 2
# 1.8: the wardrobe's presentation, first in Personalizar
adb shell input swipe 500 1800 500 700 300
shot 14w-settings-wardrobe 2
# 1.8.1: her Vestuario as a screen of its own (ClosetActivity), then back
tap "abrir su vestuario"
shot 14x-closet 12
echo "--- closet: crashes and its log"; adb logcat -d -b crash | tail -40; adb logcat -d | grep -iE "closet|AndroidRuntime|ActivityTaskManager: START" | tail -25
adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell input swipe 500 1800 500 700 300
shot 13c-style-more 2
tap "ajustes"
shot 14-settings-tab 2
# the language card, under her permissions
adb shell input swipe 500 1800 500 600 300
sleep 1
adb shell input swipe 500 1800 500 600 300
# the app in another language, then back to the phone's
tap "français"
shot 14f-french 4
tap "nouveautés"
tap "changements"
shot 14g-french-news 2
tap "réglages"
sleep 2
adb shell input swipe 500 1800 500 700 300
sleep 1
adb shell input swipe 500 1800 500 700 300
tapdown "automatique" 4
sleep 4
# what she remembers about you (empty on a fresh install), in «Tú»
tap "tú"
adb shell input swipe 500 1800 500 900 300
shot 14m-memory 3
tap "comunidad"
shot 14a-community-tab 2
tap "comandos"
shot 14b-commands-tab 2
adb shell input swipe 500 1800 500 700 300
shot 14c-commands-more 2
tap "novedades"
adb shell input swipe 500 1800 500 600 300
shot 14d-news-folded 2
tap "cambios"
shot 14e-news-open 2
adb shell input swipe 500 600 500 1800 300
tap "novedades"
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
adb shell input swipe 500 600 500 1800 300
tap "iniciar capri"
shot 15-capri-on 14
adb shell input keyevent KEYCODE_HOME
shot 16-capri-home 6
# her quick menu (as holding her, or her notification's "menu"), all her settings, her AI in two steps
adb shell am start-foreground-service -n $PKG/.CapriService -a capri.MENU
shot 16b-quick-menu 4
tap "todos los ajustes"
shot 16c-all-settings 3
tap "modelo de ia"
shot 16d-ai-model 4
adb shell input tap 20 200
sleep 2
adb shell input tap 20 200
# her chat (as "Talk to her" in her menu: a debug build lets adb ask her service), then typing
adb shell am start-foreground-service -n $PKG/.CapriService -a capri.CHAT
shot 17-chat 12
# what her panel says (her WebView's text, as accessibility sees it)
dump; python3 - <<'PY2'
import re
xml = open('/tmp/ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?class="([^"]*)"[^>]*?package="([^"]*)"[^>]*?bounds="([^"]*)"', xml):
    if 'capri' in m.group(3) and (m.group(1) or 'EditText' in m.group(2)):
        print('ui:', m.group(2), repr(m.group(1)), m.group(4))
PY2
# her text box, tapped the way you'd tap it
python3 - <<'PY2' > /tmp/xy
import re
xml = open('/tmp/ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*?class="android.widget.EditText"[^>]*?package="[^"]*capri[^"]*"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    x1, y1, x2, y2 = map(int, m.groups()); print((x1 + x2) // 2, (y1 + y2) // 2); break
PY2
[ -s /tmp/xy ] || echo "386 564" > /tmp/xy # where her box is on this screen (17-chat), if accessibility doesn't say
adb shell input tap $(cat /tmp/xy); echo "tapped her box at $(cat /tmp/xy)"
sleep 3
for k in H O L A; do adb shell input keyevent KEYCODE_$k; sleep 1; done
shot 18-typing 2
adb shell input keyevent KEYCODE_L
shot 19-typed-l 3
adb shell input text "choom"
shot 20-typed-text 3
adb shell input tap 1040 1300 # away from her (above the keyboard, beside her): her chat closes, the keyboard goes
shot 21-tap-away 3
# typing in another app (the launcher's search) with her on screen: its keyboard over her
adb shell input tap 300 2280
sleep 3
adb shell input text "hola"
shot 22-other-app 3
# her ears: the microphone allowed, "Activar" in Ajustes, then what she'd hear (a debug build lets
# adb hand her service the words: the emulator has no microphone)
adb shell input keyevent KEYCODE_BACK
adb shell pm grant $PKG android.permission.RECORD_AUDIO
adb shell am start -n $PKG/.MainActivity --ei tab 4 # straight to Ajustes (Capri may sit on the pill bar)
sleep 4
adb shell input swipe 500 1800 500 900 300
shot 23-voice-card 2
adb shell input swipe 500 1800 500 700 300
shot 23b-settings-more 2
adb shell input swipe 500 1800 500 700 300
shot 23c-settings-more 2
adb shell input swipe 500 1800 500 700 300
shot 23d-settings-more 2
adb shell input swipe 500 1800 500 700 300
shot 23e-settings-end 2
for i in 1 2 3 4 5 6 7 8; do adb shell input swipe 500 700 500 1800 200; done
tapdown "activar" 8
shot 24-voice-on 3
adb shell input keyevent KEYCODE_HOME
sleep 4
hear() { adb shell am start-foreground-service -n $PKG/.CapriService -a capri.HEAR --es text "'$1'" >/dev/null; }
hear "Capri"
shot 25-heard-name 3
hear "Capri qué hora es"
shot 26-heard-time 3
hear "Capri prende la linterna"
shot 27-heard-torch 3
hear "Capri escóndete"
shot 28-heard-hide 4
hear "Capri ven"
shot 29-heard-back 4
# her icon mode: only her mask, still talking; then back out
hear "Capri modo ícono"
shot 30-icon 9
hear "Capri sal del ícono"
shot 31-icon-off 8
# her wardrobe, by voice; a tap above it closes it. Then the secret (wedding.js), said out loud
hear "Capri abre tu vestuario"
shot 32-wardrobe 9
adb shell input tap 540 300
sleep 2
hear "Capri cásate conmigo"
shot 33-wedding-gasp 2
shot 34-wedding-yes 5
shot 35-wedding-veil 6
# 1.9.0: her sign that she's listening (her name alone), her reaction to music playing, something
# shared with her, "tradúceme esto", streaming mode on and off
adb shell input tap 540 300
sleep 2
hear "Capri"
shot 36-hearing 2
adb shell am start-foreground-service -n $PKG/.CapriService -a capri.SENSE --ez sound true --es kind '""' >/dev/null
shot 37-react-music 14
adb shell am start-foreground-service -n $PKG/.CapriService -a capri.SENSE --ez sound false --es kind '""' >/dev/null
adb shell am start -n $PKG/.ShareActivity -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "'mira esto, choom'" >/dev/null
shot 38-share-text 12
adb shell input keyevent KEYCODE_BACK
adb shell input tap 540 300
sleep 2
hear "Capri tradúceme esto al inglés"
shot 39-translate 14
adb shell input keyevent KEYCODE_BACK
adb shell input tap 540 300
sleep 2
hear "Capri modo streaming"
shot 40-streaming 5
adb shell input keyevent KEYCODE_BACK
hear "Capri quita el modo streaming"
shot 41-streaming-off 4
adb logcat -d -s capri capri-js | grep -iE "share|stream|webgl|GPU|picture|busy|answered instead|in front" | tail -20
adb shell dumpsys activity services $PKG | grep -iE "foregroundServiceType|types=" | head -4
adb logcat -d | grep -iE "capri|chromium|torch" | grep -viE "regions|keyboard" | tail -40
adb shell dumpsys window windows | grep -E "Window #" | head -16
adb shell dumpsys window windows | grep -E "Window #|mFrame=|mAttrs=.*ty=" | grep -B1 -A1 -iE "capri|InputMethod" | head -40
adb shell wm size; adb shell wm density
adb logcat -d -s capri capri-js chromium | grep -v "regions" | tail -80
adb logcat -d -s capri | grep -E "keyboard|size|regions" | tail -30
# her memory: idle on the home screen for a while, then what Android says she uses (PSS, MB)
adb shell input keyevent KEYCODE_HOME
sleep 45
echo "===MEMINFO==="
for i in 1 2 3; do
  app=$(adb shell pidof $PKG | tr -d '\r')
  web=$(adb shell top -b -n 1 | grep sandboxed_process | awk '{print $1}' | head -1)
  a=$(adb shell dumpsys meminfo $app | grep "TOTAL PSS" | awk '{print $3}')
  # Android lists RSS first, then PSS: the second match is her web engine's PSS
  w=$(adb shell dumpsys meminfo | grep "K: com.google.android.webview:sandboxed" | sed -n 2p | awk '{print $1}' | tr -d 'K,:')
  w=${w:-0}; a=${a:-0}
  echo "pss app=$a web=$w total=$(( (a + w) / 1024 )) MB"
  sleep 10
done
t1=$(adb shell cat /proc/$app/stat | awk '{print $14+$15}'); w1=$(adb shell cat /proc/$web/stat | awk '{print $14+$15}')
sleep 30
t2=$(adb shell cat /proc/$app/stat | awk '{print $14+$15}'); w2=$(adb shell cat /proc/$web/stat | awk '{print $14+$15}')
echo "cpu ticks in 30 s idle: app=$((t2 - t1)) web=$((w2 - w1)) (100 per second = one core)"
adb shell dumpsys gfxinfo $PKG reset >/dev/null
sleep 20
echo "frames drawn in 20 s idle:"; adb shell dumpsys gfxinfo $PKG | grep -E "Total frames" | head -2
adb shell top -b -n 1 -H -p $app | head -20
echo "===END MEMINFO==="
