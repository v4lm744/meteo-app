-keepnames class com.meteoapp.data.model.Coord
-if class com.meteoapp.data.model.Coord
-keep class com.meteoapp.data.model.CoordJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
