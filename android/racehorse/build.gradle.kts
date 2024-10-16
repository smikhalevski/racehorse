plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

tasks.dokkaHtml.configure {
    outputDirectory.set(file("../../docs/android"))
}

android {
    namespace = "org.racehorse"
    compileSdk = 34

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
    implementation("androidx.activity:activity:1.9.2")

    // EventBridge
    compileOnly("org.greenrobot:eventbus:3.3.1")
    compileOnly("com.google.code.gson:gson:2.10.1")

    // ActivityPlugin
    compileOnly("androidx.lifecycle:lifecycle-process:2.8.5")

    // AssetLoaderPlugin
    compileOnly("androidx.webkit:webkit:1.11.0")

    // DevicePlugin
    compileOnly("androidx.appcompat:appcompat:1.7.0")

    // Google Sign-In
    compileOnly("com.google.android.gms:play-services-auth:21.2.0")

    // Facebook Login
    compileOnly("com.facebook.android:facebook-login:latest.release")

    // Google Play referrer
    compileOnly("com.android.installreferrer:installreferrer:2.2")

    // Google Pay
    compileOnly("com.google.android.gms:play-services-tapandpay:18.3.3")

    // Push notifications
    compileOnly("com.google.firebase:firebase-messaging-ktx:24.0.1")

    // Biometric
    compileOnly("androidx.biometric:biometric:1.2.0-alpha05")

    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp:mockwebserver:1.2.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
