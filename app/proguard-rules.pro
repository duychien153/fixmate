# Gson relies on reflection for DTO field names.
-keep class com.duychien.fixmate.core.network.dto.** { *; }
-keepattributes Signature, *Annotation*

# Retrofit
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
