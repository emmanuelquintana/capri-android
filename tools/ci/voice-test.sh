#!/bin/bash
# (1.11.3) «Bajar su voz» and one of her phrases out loud, on an emulator: VoiceSelfTest through the debug-only
# service action, read back from logcat (CapriVoiceTest).
set -u
PKG=io.github.emmanuelquintana.capri
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
# media volume up (a phone on 0 says nothing: the app tells you so, but here we want to hear her)
adb shell cmd media_session volume --stream 3 --set 10 >/dev/null 2>&1 || adb shell media volume --stream 3 --set 10 >/dev/null 2>&1
adb logcat -c
adb shell am start-foreground-service -n $PKG/.CapriService -a capri.VOICETEST
seen=0
for i in $(seq 1 240); do # hasta 20 min
  sleep 5
  adb logcat -d -s CapriVoiceTest:I | grep CapriVoiceTest > now.log
  n=$(wc -l < now.log); if [ "$n" -gt "$seen" ]; then tail -n $((n - seen)) now.log; seen=$n; fi
  if grep -qE "DONE|FAIL" now.log; then break; fi
done
adb logcat -d | grep -iE "FATAL|AndroidRuntime: |MediaPlayer|CapriVoice" | grep -v CapriVoiceTest | tail -30
grep -q "DONE ok" now.log
