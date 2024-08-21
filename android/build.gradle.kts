plugins {
    id("com.android.application") version "8.4.1" apply false
    id("com.android.library") version "8.4.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.2")
    }
}
