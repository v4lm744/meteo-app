-keepnames class com.meteoapp.data.model.WeatherData
-if class com.meteoapp.data.model.WeatherData
-keep class com.meteoapp.data.model.WeatherDataJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
