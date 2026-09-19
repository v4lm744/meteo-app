-keepnames class com.meteoapp.data.model.ForecastResponse
-if class com.meteoapp.data.model.ForecastResponse
-keep class com.meteoapp.data.model.ForecastResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.ForecastResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.ForecastResponse {
    public synthetic <init>(java.lang.String,java.lang.Long,java.lang.Long,java.util.List,com.meteoapp.data.model.CityInfo,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
