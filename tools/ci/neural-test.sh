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
grep -q "DONE" neural.log
