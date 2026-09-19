-keepnames class com.meteoapp.data.model.MainMetrics
-if class com.meteoapp.data.model.MainMetrics
-keep class com.meteoapp.data.model.MainMetricsJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.MainMetrics
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.MainMetrics {
    public synthetic <init>(double,double,java.lang.Double,java.lang.Double,long,long,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
