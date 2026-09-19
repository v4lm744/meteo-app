-keepnames class com.meteoapp.data.model.CityInfo
-if class com.meteoapp.data.model.CityInfo
-keep class com.meteoapp.data.model.CityInfoJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.CityInfo
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.CityInfo {
    public synthetic <init>(java.lang.Long,java.lang.String,com.meteoapp.data.model.Coord,java.lang.String,java.lang.Long,java.lang.Long,java.lang.Long,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
