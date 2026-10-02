# Room entity, DAO, and model classes
-keep class com.daymark.app.data.** { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-if @kotlinx.serialization.Serializable class *
-keep class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class *
-keepclassmembers class <1> {
    kotlinx.serialization.KSerializer serializer(...);
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class *
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }
