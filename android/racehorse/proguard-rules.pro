-keepnames class org.racehorse.** extends java.io.Serializable { *; }

-keepclassmembers class org.racehorse.** extends java.io.Serializable { *; }

-keepclassmembers class org.racehorse.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Facebook Login
-keepnames class com.facebook.AccessToken { *; }

-keepclassmembers class com.facebook.AccessToken { *; }
