-keepnames class com.meteoapp.data.model.CurrentData
-if class com.meteoapp.data.model.CurrentData
-keep class com.meteoapp.data.model.CurrentDataJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
