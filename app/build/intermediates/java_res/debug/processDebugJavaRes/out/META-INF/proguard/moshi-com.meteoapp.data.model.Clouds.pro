-keepnames class com.meteoapp.data.model.Clouds
-if class com.meteoapp.data.model.Clouds
-keep class com.meteoapp.data.model.CloudsJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
