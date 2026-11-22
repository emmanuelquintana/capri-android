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
adb logcat -d | grep -iE "FATAL|AndroidRuntime: |MediaPlayer|CapriVoice" | grep -v "CapriVoiceTest\|CapriVoiceEvent" | tail -30
# what Capri herself did with each phrase (Ears: VOICE_STARTED … VOICE_FINISHED, or why not)
adb logcat -d -s CapriVoiceEvent:I | grep CapriVoiceEvent | tee events.log
grep -q "DONE ok" now.log || exit 1
# each phrase through Capri: started and finished
for id in st-id-es-MX st-words-es-MX st-id-en-US st-words-en-US; do
  grep "\"$id\"" events.log | grep -q VOICE_FINISHED && echo "ok $id" || { echo "FAIL $id"; exit 1; }
done
# (1.11.5) her icon: switch to one of hers, open the app through it, back to the original, open it again
icon() { adb logcat -c; adb shell am start-foreground-service -n $PKG/.CapriService -a capri.ICONTEST "$@" >/dev/null; sleep 4
  adb logcat -d -s CapriIconTest:I | grep -o "launcher=.*"; }
opens() { adb shell am start -W -n $PKG/$1 | grep -q "Status: ok" && sleep 2 && adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | grep -qE "$PKG/\.(Home|OnboardingActivity)" \
  && echo "ok opens $1" || { echo "FAIL opens $1"; exit 1; }; adb shell input keyevent HOME; }
l=$(icon --es face capri --es shape round); echo "$l"; [ "$l" = "launcher=Icon_capri_round" ] || { echo "FAIL icon switch"; exit 1; }
opens .Icon_capri_round
l=$(icon); echo "$l"; [ "$l" = "launcher=MainActivity" ] || { echo "FAIL icon back"; exit 1; }
opens .MainActivity
