-keepnames class com.meteoapp.data.model.HourlyData
-if class com.meteoapp.data.model.HourlyData
-keep class com.meteoapp.data.model.HourlyDataJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.HourlyData
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.HourlyData {
    public synthetic <init>(long,double,double,long,double,long,java.util.List,java.lang.Double,long,long,long,java.lang.Long,double,double,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
