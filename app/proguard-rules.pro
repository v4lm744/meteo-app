# Moshi : modeles generes par KSP (codegen), pas de reflexion a conserver.
# On garde seulement les classes si l'API masque des champs via @Json.
-keepclassmembers class com.meteoapp.data.model.** {
    <init>();
}

# Retrofit : signatures generiques et annotations de service
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn kotlinx.coroutines.**

# Osmdroid : cartographie et overlays references de facon dynamique
-dontwarn org.osmdroid.**

# Coroutines : noms de classes de debug supprimes, pas le code
-dontwarn kotlinx.coroutines.debug.**

# Widgets et workers : instancies par le systeme via le manifest
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.Worker { *; }
