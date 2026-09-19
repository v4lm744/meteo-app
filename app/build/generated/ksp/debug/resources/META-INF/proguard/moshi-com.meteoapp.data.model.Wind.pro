-keepnames class com.meteoapp.data.model.Wind
-if class com.meteoapp.data.model.Wind
-keep class com.meteoapp.data.model.WindJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
