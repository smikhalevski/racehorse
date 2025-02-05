-keepclassmembers class org.racehorse.** {
    @android.webkit.JavascriptInterface <methods>;
}

-keepnames @kotlinx.serialization.Serializable class **

-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-if class **.*$Companion {
  kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class <1>.<2> {
  <1>.<2>$Companion Companion;
}
