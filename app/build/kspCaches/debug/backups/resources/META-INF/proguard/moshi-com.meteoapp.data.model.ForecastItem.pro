-keepnames class com.meteoapp.data.model.ForecastItem
-if class com.meteoapp.data.model.ForecastItem
-keep class com.meteoapp.data.model.ForecastItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.ForecastItem
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.ForecastItem {
    public synthetic <init>(long,com.meteoapp.data.model.MainMetrics,java.util.List,com.meteoapp.data.model.Clouds,com.meteoapp.data.model.Wind,java.lang.Double,com.meteoapp.data.model.RainVolume,com.meteoapp.data.model.SnowVolume,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
