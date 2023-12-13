plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example"
    compileSdk = 34

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
    // EventBridge
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("com.google.code.gson:gson:2.10.1")

    // LifecyclePlugin
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")

    // AssetLoaderPlugin
    implementation("androidx.webkit:webkit:1.9.0")

    // DevicePlugin
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation(platform("com.google.firebase:firebase-bom:32.1.0"))

    // Facebook Login
    implementation("com.facebook.android:facebook-login:latest.release")

    // Firebase
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation(project(":racehorse"))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
