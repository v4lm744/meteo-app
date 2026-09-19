-keepnames class com.meteoapp.data.model.GeoLocation
-if class com.meteoapp.data.model.GeoLocation
-keep class com.meteoapp.data.model.GeoLocationJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.GeoLocation
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.GeoLocation {
    public synthetic <init>(java.lang.String,com.meteoapp.data.model.LocalNames,double,double,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
