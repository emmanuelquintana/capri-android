# Componentes de voz aislados

`voice-components` fija el repositorio privado [capri-voice-packs](https://github.com/emmanuelquintana/capri-voice-packs) como submódulo. Incluye las fuentes Qwen C++/GGML y Chatterbox, perfiles autorizados para este proyecto privado, pruebas, una app independiente de guiones y la entrega para Claude.

Esta rama añade únicamente el enlace y este documento. La aplicación no incluye ni activa el módulo; no cambia `Ears.kt`, Gradle, permisos ni el TTS actual. La compilación normal no necesita inicializar el submódulo. Los recursos vocales permanecen en el repositorio privado y requieren acceso autorizado.

```powershell
git submodule update --init voice-components
cd voice-components/adapters/android
./gradlew.bat :qwen-voice:assembleRelease :qwen-voice:testDebugUnitTest
```

Requisitos: JDK 17+, SDK 35, NDK 27.2.12479018 y CMake 3.22.1. El AAR neural requiere Android 12/API 31+, ARM64 o x86_64. El motor se compila con la app; la futura descarga opcional llevará solo datos de modelos y perfiles, verificados por tamaño y SHA-256. Los GGUF se comparten entre voces y no se duplican por perfil.

Claude debe empezar por `voice-components/docs/CLAUDE_INTEGRATION.md`, luego `ANDROID_HANDOFF.md` y `CATALOG_AND_CLIENTS.md`. El catálogo y los modelos están en releases del mismo repositorio privado; no introducir un token personal compartido en el APK.

**Fidelidad sin aprobar:** el usuario rechazó las pruebas latinas. Consultar `FIDELITY_STATUS.md` antes de integrar. En emulador x86_64 se observaron 927,5 MB PSS de pico y 89,3 MB entre respuestas; falta medir un teléfono ARM físico. No prometer esos valores en todos los dispositivos.

Validación de esta rama: la aplicación original compiló con `:app:assembleDebug`. Las pruebas del módulo independiente y sus límites están en `voice-components/docs/VALIDATION.md`.
