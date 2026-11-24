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

# (1.12.0) Vosk (her passive ears) reaches its native library through JNA, which finds classes, fields and
# methods by name
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
-dontwarn java.awt.**
-dontwarn com.sun.jna.**

# (1.12.0) "Entrar con Google" (Account.kt): Credential Manager finds its Play services provider by name
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
