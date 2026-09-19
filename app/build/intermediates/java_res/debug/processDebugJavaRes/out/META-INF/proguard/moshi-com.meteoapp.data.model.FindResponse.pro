-keepnames class com.meteoapp.data.model.FindResponse
-if class com.meteoapp.data.model.FindResponse
-keep class com.meteoapp.data.model.FindResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.FindResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.FindResponse {
    public synthetic <init>(java.lang.String,java.lang.Long,java.util.List,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
