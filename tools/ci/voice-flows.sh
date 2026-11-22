#!/bin/bash
# (1.11.7, Emma: "No habla… solo escucho su voz cuando le doy al botón de probar") Her voice in the moments
# people really hear her, not the test button: after voice-test.sh (her voice downloaded, Capri on), with the
# phone on VIBRATE as most phones are:
#   1. a moment of hers (one of her lines from lines.js said as she says it: voicelines.js) → her phrase plays
#   2. a message in the app's Chat tab (appchat.js) → her answer is said out loud (chatvoice.js → Ears)
#   3. "Capri, …" heard (listen.js) → her answer is said out loud
set -u
PKG=io.github.emmanuelquintana.capri
flow() { adb shell am start-foreground-service -n $PKG/.CapriService -a capri.FLOWTEST "$@" >/dev/null; }
wait_ev() { # id-pattern, seconds → the events of that id, until it ends
  for i in $(seq 1 $(( $2 / 3 ))); do
    sleep 3
    adb logcat -d -s CapriVoiceEvent:I | grep CapriVoiceEvent | grep -E "\"id\":\"$1" > ev.log
    grep -qE "VOICE_FINISHED|VOICE_ERROR|VOICE_UNAVAILABLE|VOICE_CANCELLED" ev.log && break
  done
  sed 's/^.*CapriVoiceEvent: //' ev.log
}
fail=0
adb logcat -c
flow --es do ringer --es mode vibrate; sleep 2
adb logcat -d -s CapriVoiceTest:I | grep -o "ringer.*"
adb shell input keyevent HOME; sleep 2

echo "== 1. a moment of hers (anger), on vibrate"
adb logcat -c
flow --es do moment --es pool thrown.angry
wait_ev clip- 40
adb logcat -d -s capri-js:D | grep "debug moment" | tail -1
grep VOICE_FINISHED ev.log | grep -q '"source":"clip"' && echo "ok moment" || { echo "FAIL moment"; fail=1; }

echo "== 2. the app's Chat tab"
adb logcat -c
flow --es do chat --es text "Capri, join back together"
wait_ev chat- 90
grep -q VOICE_REQUESTED ev.log && grep -qE "VOICE_STARTED|VOICE_FINISHED" ev.log && echo "ok app chat" || { echo "FAIL app chat"; fail=1; }

echo "== 3. \"Capri, …\" heard"
adb logcat -c
adb shell am start-foreground-service -n $PKG/.CapriService -a capri.HEAR --es text "'Capri what time is it'" >/dev/null
wait_ev answer- 60
grep -q VOICE_REQUESTED ev.log && grep -qE "VOICE_STARTED|VOICE_FINISHED" ev.log && echo "ok heard" || { echo "FAIL heard"; fail=1; }

flow --es do ringer --es mode normal
adb logcat -d | grep -iE "FATAL|AndroidRuntime: |capri-js.*(Error|error)" | tail -15
exit $fail
