// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false

    // ## AÑADE ESTA LÍNEA ##
    id("androidx.navigation.safeargs.kotlin") version "2.8.9" apply false


}