plugins {
    id("com.android.library")
    id("maven-publish")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

tasks.dokkaHtml.configure {
    outputDirectory.set(file("../../docs/android"))
}

android {
    namespace = "org.racehorse"
    compileSdk = 35

    defaultConfig {
        // https://apilevels.com/
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        create("staging") {
            initWith(getByName("debug"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "org.racehorse"
            artifactId = "racehorse"
            version = "1.8.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "ghcr"
            url = uri("https://maven.pkg.github.com/smikhalevski/racehorse")

            credentials {
                username = "smikhalevski"
                password = System.getenv("GH_PAT")
            }
        }
    }
}

dependencies {
    implementation(kotlin("reflect"))

    // Edge-to-edge
    implementation("androidx.activity:activity:1.10.0")

    // Serialization
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // EventBridge
    compileOnly("org.greenrobot:eventbus:3.3.1")

    // ActivityPlugin
    compileOnly("androidx.lifecycle:lifecycle-process:2.8.7")

    // AssetLoaderPlugin
    compileOnly("androidx.webkit:webkit:1.12.1")

    // DevicePlugin
    compileOnly("androidx.appcompat:appcompat:1.7.0")

    // Google Sign-In
    compileOnly("com.google.android.gms:play-services-auth:21.3.0")

    // Facebook Login
    compileOnly("com.facebook.android:facebook-login:latest.release")

    // Google Play referrer
    compileOnly("com.android.installreferrer:installreferrer:2.2")

    // Google Pay
    compileOnly("com.google.android.gms:play-services-tapandpay:18.3.3")

    // Push notifications
    compileOnly("com.google.firebase:firebase-messaging-ktx:24.1.0")

    // Biometric
    compileOnly("androidx.biometric:biometric:1.2.0-alpha05")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp:mockwebserver:1.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
}
