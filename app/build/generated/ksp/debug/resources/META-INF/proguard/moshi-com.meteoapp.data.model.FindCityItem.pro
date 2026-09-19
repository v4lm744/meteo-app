-keepnames class com.meteoapp.data.model.FindCityItem
-if class com.meteoapp.data.model.FindCityItem
-keep class com.meteoapp.data.model.FindCityItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.FindCityItem
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.FindCityItem {
    public synthetic <init>(long,java.lang.String,com.meteoapp.data.model.Coord,com.meteoapp.data.model.MainMetrics,java.util.List,com.meteoapp.data.model.Sys,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
