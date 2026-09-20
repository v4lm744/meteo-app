# Modèles météo désérialisés via Moshi (réflexion absente en mode minifié)
-keep class com.meteoapp.data.model.** { *; }

# Moshi : adapters générés par KSP et classes annotées
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class com.meteoapp.data.model.** {
    <fields>;
    <init>();
}
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
    @com.squareup.moshi.JsonClass <init>(...);
}
-dontwarn com.squareup.moshi.**

# Retrofit : signatures génériques et annotations de service
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

# Osmdroid : cartographie et overlays référencés de façon dynamique
-dontwarn org.osmdroid.**
-dontwarn org.mapsforge.**

# Glide : modules chargés par réflexion
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$** { *; }
-dontwarn com.bumptech.glide.**

# Coroutines : débogage des noms de classes supprimé, pas le code
-dontwarn kotlinx.coroutines.debug.**

# Widgets et workers : instanciés par le système via le manifest
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.Worker { *; }
