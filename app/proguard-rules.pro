# The page calls these by name (HostBridge.kt)
-keepclassmembers class io.github.emmanuelquintana.capri.HostBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Open source: class and method names stay readable in the APK (no renaming), so anyone (and any
# antivirus) can see what it does. Unused code is still removed.
-dontobfuscate
# JNI resolves class names, constructors and private parameter fields by name.
-keep class com.qwen.tts.studio.engine.** { *; }

-keep class io.github.emmanuelquintana.voicefx.CapriCyberVoiceFX { *; }
