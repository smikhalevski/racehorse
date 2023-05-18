-keepnames class org.racehorse.** extends java.io.Serializable { *; }

-keepclassmembers class org.racehorse.** extends java.io.Serializable { *; }

-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}
