plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example"
        // https://apilevels.com/
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("example") {
            storeFile = file("example-keystore.jks")
            storePassword = "example"
            keyAlias = "key0"
            keyPassword = "example"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("example")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs.getByName("example")
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Edge-to-edge
    implementation("androidx.activity:activity:1.10.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // EventBridge
    implementation("org.greenrobot:eventbus:3.3.1")

    // ActivityPlugin
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    // AssetLoaderPlugin
    implementation("androidx.webkit:webkit:1.12.1")

    // DevicePlugin
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))

    // Facebook Login
    implementation("com.facebook.android:facebook-login:latest.release")

    // Firebase
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation(project(":racehorse"))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
