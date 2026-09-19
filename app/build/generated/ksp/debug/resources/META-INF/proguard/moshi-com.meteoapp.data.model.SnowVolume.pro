-keepnames class com.meteoapp.data.model.SnowVolume
-if class com.meteoapp.data.model.SnowVolume
-keep class com.meteoapp.data.model.SnowVolumeJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.SnowVolume
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.SnowVolume {
    public synthetic <init>(java.lang.Double,java.lang.Double,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
