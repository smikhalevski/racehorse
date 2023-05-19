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
    compileSdk = 33

    defaultConfig {
        // https://apilevels.com/
        minSdk = 26
        targetSdk = 33

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
            version = "0.0.7"

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
    // Android
    compileOnly("androidx.appcompat:appcompat:1.6.1")
    compileOnly("androidx.webkit:webkit:1.6.1")

    // EventBridge
    compileOnly("org.greenrobot:eventbus:3.3.1")
    compileOnly("com.google.code.gson:gson:2.8.9")

    // Google Play referrer
    compileOnly("com.android.installreferrer:installreferrer:2.2")

    // Push notifications
    compileOnly("com.google.firebase:firebase-messaging-ktx:23.1.2")

    testImplementation("com.google.code.gson:gson:2.8.9")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp:mockwebserver:1.2.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
