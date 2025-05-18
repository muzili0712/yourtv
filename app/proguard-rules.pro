# 保留 ReqSourceAdd，允许混淆
-keep,allowobfuscation class com.horsenma.yourtv.data.ReqSourceAdd {
    *;
    <init>(...);
}

# 保留 data class 字段，允许方法混淆
-keepclassmembers class com.horsenma.yourtv.data.** {
    private <fields>;
    public <fields>;
    <init>(...);
    *** component*();
    *** copy(...);
}
-keep,allowobfuscation class com.horsenma.yourtv.data.** {
    <init>(...);
}

# 保留 decoder，允许混淆
-keep,allowobfuscation class com.horsenma.yourtv.SourceDecoder {
    public static <methods>;
    <init>(...);
}
-keep,allowobfuscation class com.horsenma.yourtv.SourceEncoder {
    public static <methods>;
    <init>(...);
}

# Gson 相关
-keep class com.google.gson.** { *; }
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class com.horsenma.yourtv.data.ReqSources { <fields>; <init>(...); }
-keep class com.horsenma.yourtv.data.Source { <fields>; <init>(...); }
-keep class com.horsenma.yourtv.data.TV { <fields>; <init>(...); }
-keep class com.horsenma.yourtv.data.EPG { <fields>; <init>(...); }

# 日志移除
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
-keep class android.util.Log {
    public static int e(...);
}

# 必要库
-keep class okhttp3.OkHttpClient { *; }
-keep class okhttp3.Request { *; }
-keep class okhttp3.Response { *; }
-keep class androidx.media3.** { *; }
-keep class com.bumptech.glide.** { *; }
-keep class com.google.zxing.** { *; }
-keep class fi.iki.elonen.** { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class com.horsenma.yourtv.databinding.** { *; }