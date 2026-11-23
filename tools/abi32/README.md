# Marcador de 32 bits

`libcapri_abi32.so` (armeabi-v7a y x86) no hace nada: solo le dice al instalador de Android que el APK sirve en
procesadores de 32 bits. Sin una carpeta `lib/armeabi-v7a/` el instalador rechaza el APK en esos teléfonos
(«CPU incompatible»), porque el motor de voz trae sus librerías solo para 64 bits.

En los teléfonos de 64 bits el sistema elige `arm64-v8a` / `x86_64` como siempre. En los de 32 bits el motor de voz
dice que no es compatible (`CapriNeural.unsupported() == "abi"`) y Capri usa sus frases grabadas y la voz del teléfono.

Para rehacerlo:

```sh
clang --target=armv7a-linux-androideabi21 -O2 -fPIC -shared -nostdlib -fuse-ld=lld -Wl,-z,max-page-size=16384 \
  -Wl,--hash-style=both -Wl,-soname,libcapri_abi32.so -o app/src/main/jniLibs/armeabi-v7a/libcapri_abi32.so tools/abi32/stub.c
clang --target=i686-linux-android21 -O2 -fPIC -shared -nostdlib -fuse-ld=lld -Wl,-z,max-page-size=16384 \
  -Wl,--hash-style=both -Wl,-soname,libcapri_abi32.so -o app/src/main/jniLibs/x86/libcapri_abi32.so tools/abi32/stub.c
```
