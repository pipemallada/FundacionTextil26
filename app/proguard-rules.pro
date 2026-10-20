# Gson necesita los nombres reales de los campos de los DTO para armar el JSON de la API
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class cl.duoc.redtextil.data.remote.dto.** { *; }

# Retrofit con funciones suspend
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation interface cl.duoc.redtextil.data.remote.RedTextilApiService

-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
