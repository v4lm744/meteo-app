-keepnames class com.meteoapp.data.model.DailyData
-if class com.meteoapp.data.model.DailyData
-keep class com.meteoapp.data.model.DailyDataJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
