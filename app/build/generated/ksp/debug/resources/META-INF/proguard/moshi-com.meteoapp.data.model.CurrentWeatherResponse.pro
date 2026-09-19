-keepnames class com.meteoapp.data.model.CurrentWeatherResponse
-if class com.meteoapp.data.model.CurrentWeatherResponse
-keep class com.meteoapp.data.model.CurrentWeatherResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.CurrentWeatherResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.CurrentWeatherResponse {
    public synthetic <init>(com.meteoapp.data.model.Coord,java.util.List,java.lang.String,com.meteoapp.data.model.MainMetrics,java.lang.Long,com.meteoapp.data.model.Wind,com.meteoapp.data.model.Clouds,long,com.meteoapp.data.model.Sys,java.lang.Long,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
