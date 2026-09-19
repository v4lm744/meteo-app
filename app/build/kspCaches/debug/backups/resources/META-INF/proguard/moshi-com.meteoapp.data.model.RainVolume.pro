-keepnames class com.meteoapp.data.model.RainVolume
-if class com.meteoapp.data.model.RainVolume
-keep class com.meteoapp.data.model.RainVolumeJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.RainVolume
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.RainVolume {
    public synthetic <init>(java.lang.Double,java.lang.Double,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
