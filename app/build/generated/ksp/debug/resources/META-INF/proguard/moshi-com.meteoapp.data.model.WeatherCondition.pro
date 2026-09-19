-keepnames class com.meteoapp.data.model.WeatherCondition
-if class com.meteoapp.data.model.WeatherCondition
-keep class com.meteoapp.data.model.WeatherConditionJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
