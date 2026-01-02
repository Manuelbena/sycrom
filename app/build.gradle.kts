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
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {

    // --- LIBRERÍA CALENDARIO (MAVEN CENTRAL - NO FALLA) ---
    implementation("com.kizitonwose.calendar:view:2.6.0")

    // Desugaring para fechas Java 8 en Android antiguos (Recomendado por la librería)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("com.airbnb.android:lottie:6.4.1")
    val room_version = "2.6.1" // Última versión estable de Room
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Para usar Coroutines con Room
    ksp("androidx.room:room-compiler:$room_version") // Usamos ksp para el procesador de anotaciones

    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    implementation("androidx.work:work-runtime-ktx:2.9.0") // O la versión que uses
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // --- DEPENDENCIAS DE HILT ---
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")

    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")
    implementation("com.google.guava:guava:31.1-android")


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
