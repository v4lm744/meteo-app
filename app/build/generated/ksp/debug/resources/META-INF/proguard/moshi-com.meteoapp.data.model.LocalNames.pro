-keepnames class com.meteoapp.data.model.LocalNames
-if class com.meteoapp.data.model.LocalNames
-keep class com.meteoapp.data.model.LocalNamesJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.meteoapp.data.model.LocalNames
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.meteoapp.data.model.LocalNames {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
