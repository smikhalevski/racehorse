pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Google Pay
        maven {
            url = java.net.URI("file:${rootDir}/tapandpay_sdk/")
        }
    }
}

rootProject.name = "racehorse"

include(":racehorse")
include(":example")
