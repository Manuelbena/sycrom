// Archivo: C:/Users/manue/AndroidStudioProjects/Synkrn/app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android") // Mantén solo una declaración
    id("kotlin-kapt")
    // La línea duplicada 'id("com.google.dagger.hilt.android")' ha sido eliminada
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.manuelbena.synkron"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.manuelbena.synkron"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    val room_version = "2.6.1" // Última versión estable de Room
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Para usar Coroutines con Room
    ksp("androidx.room:room-compiler:$room_version") // Usamos ksp para el procesador de anotaciones


    // --- DEPENDENCIAS DE HILT ---
    // 3. Añadir la librería principal de Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    // 4. Añadir el compilador de Hilt usando KAPT
    kapt("com.google.dagger:hilt-compiler:2.50")


    // --- DEPENDENCIAS EXISTENTES ---
    implementation(libs.google.gson) // <-- AÑADE ESTA LÍNEA
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.compose.google.fonts)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
