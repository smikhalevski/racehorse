-keepnames class org.racehorse.** extends java.io.Serializable { *; }

-keepclassmembers class org.racehorse.** extends java.io.Serializable { *; }

-keepclassmembers class org.racehorse.** {
    @android.webkit.JavascriptInterface <methods>;
}
