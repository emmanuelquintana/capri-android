/* Capri: marks the 32-bit processors (armeabi-v7a, x86) as supported, so the APK installs there.
   The neural voice engine is 64-bit only; on these phones Capri uses her recorded phrases and the phone's voice. */
int capri_abi32_marker(void) { return 32; }
