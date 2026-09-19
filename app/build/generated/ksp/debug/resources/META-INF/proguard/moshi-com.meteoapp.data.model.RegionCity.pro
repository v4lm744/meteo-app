-keepnames class com.meteoapp.data.model.RegionCity
-if class com.meteoapp.data.model.RegionCity
-keep class com.meteoapp.data.model.RegionCityJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
