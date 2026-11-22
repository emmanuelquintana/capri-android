#!/bin/bash
# (V8.5) Her neural voice on an emulator: the APK, the model (from CI's cache), a technical fixture sound
# (a synthetic tone, NOT Capri's voice: her profile is private), then CapriNeural.selfTest through the
# debug-only service action, read back from logcat.
set -u
PKG=io.github.emmanuelquintana.capri
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
EXT=/sdcard/Android/data/$PKG/files
adb shell mkdir -p $EXT
if [ "${NEURAL_MODELS:-}" != "" ]; then
  for f in qwen-talker-0.6b-base-Q4_K_M.gguf qwen-tokenizer-12hz-Q4_K_M.gguf; do adb push "$NEURAL_MODELS/$f" $EXT/ >/dev/null && echo "pushed $f"; done
fi
adb push fixture.wav $EXT/ >/dev/null
# quick: model load, her ICL prompt and a short phrase with its progress (CI's emulator is far too slow for more)
adb shell touch $EXT/quick
adb logcat -c
adb shell am start-foreground-service -n $PKG/.CapriService -a capri.NEURALTEST
seen=0
for i in $(seq 1 1200); do # hasta 100 min: en el emulador x86_64 de CI (sin aceleración) una frase tarda ~10 min
  sleep 5
  # lo que va saliendo, al momento (si el trabajo se corta por tiempo, queda en el registro hasta dónde llegó)
  adb logcat -d -s CapriNeuralTest:I | grep CapriNeuralTest > now.log
  n=$(wc -l < now.log); if [ "$n" -gt "$seen" ]; then tail -n $((n - seen)) now.log; seen=$n; fi
  if grep -qE "DONE|FAIL" now.log; then break; fi
done
adb logcat -d -s CapriNeuralTest:I | grep CapriNeuralTest | tee neural.log
adb logcat -d | grep -iE "FATAL|AndroidRuntime: |libc  |CapriNeural|qwen" | tail -30
for l in es-MX en-US; do adb pull $EXT/neural-$l.wav . 2>/dev/null && ls -la neural-$l.wav; done
grep -q "DONE" neural.log || exit 1
# (1.11.7) her neural voice the way her answers reach it: Ears.say (kind answer) → the engine her voice mode
# picks (automatic: the neural one, now installed by the test above) → out loud, start to end
if grep -q "DONE ok" neural.log; then
  adb shell cmd media_session volume --stream 3 --set 10 >/dev/null 2>&1 || true
  adb logcat -c
  adb shell am start-foreground-service -n $PKG/.CapriService -a capri.FLOWTEST --es do say --es id neural-answer --es lang en-US --es text "'Hi, choom.'"
  for i in $(seq 1 240); do # hasta 20 min
    sleep 5
    adb logcat -d -s CapriVoiceEvent:I | grep '"neural-answer"' > ev.log
    if grep -qE "VOICE_FINISHED|VOICE_ERROR|VOICE_UNAVAILABLE|VOICE_CANCELLED" ev.log; then break; fi
  done
  cat ev.log
  adb logcat -d -s capri:W | grep -E "ran to|voice qwen" | tail -5
  grep -q VOICE_FINISHED ev.log && grep -q qwen-mobile ev.log && echo "ok neural answer" || { echo "FAIL neural answer"; exit 1; }
fi
