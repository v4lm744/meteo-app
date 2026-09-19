-keepnames class com.meteoapp.data.model.Sys
-if class com.meteoapp.data.model.Sys
-keep class com.meteoapp.data.model.SysJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
