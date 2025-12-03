pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack es necesario para android-week-view
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Synkrón"
include(":app")