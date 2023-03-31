import com.android.builder.core.BuilderConstants

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

kapt.correctErrorTypes = true

android {
    compileSdk = 33

    // https://developer.android.com/studio/releases/build-tools
    buildToolsVersion = "33.0.1"

    defaultConfig {
        applicationId = "io.github.bgavyus.lightningcamera"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 24
        targetSdk = 33
        versionCode = 12
        versionName = "0.1.2"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    buildTypes {
        getByName(BuilderConstants.RELEASE) {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            resValue("bool", "firebase_crashlytics_collection_enabled", "true")
            resValue("bool", "firebase_analytics_collection_deactivated", "false")
        }

        getByName(BuilderConstants.DEBUG) {
            withGroovyBuilder {
                "FirebasePerformance" {
                    invokeMethod("setInstrumentationEnabled", false)
                }
            }

            resValue("bool", "firebase_crashlytics_collection_enabled", "false")
            resValue("bool", "firebase_analytics_collection_deactivated", "true")
        }
    }

    compileOptions.isCoreLibraryDesugaringEnabled = true

    kotlinOptions.freeCompilerArgs = listOf(
        "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-Xopt-in=kotlinx.coroutines.FlowPreview",
        "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
    )

    packagingOptions.resources.excludes += listOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "DebugProbesKt.bin",
    )

    // https://developer.android.com/jetpack/androidx/releases/compose
    composeOptions.kotlinCompilerExtensionVersion = "1.4.4"

    testOptions.unitTests.isReturnDefaultValues = true
    namespace = "io.github.bgavyus.lightningcamera"
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    val kotlinVersion: String by rootProject.extra
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")

    // https://github.com/Kotlin/kotlinx.coroutines/releases
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    val hiltVersion: String by rootProject.extra
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")

    // https://developer.android.com/jetpack/androidx/releases/core
    implementation("androidx.core:core-ktx:1.9.0")

    // https://developer.android.com/jetpack/androidx/releases/fragment
    implementation("androidx.fragment:fragment-ktx:1.5.6")

    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    // https://developer.android.com/jetpack/androidx/releases/constraintlayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // https://natario1.github.io/Egloo/about/changelog
    implementation("com.otaliastudios.opengl:egloo:0.6.1")

    // https://firebase.google.com/support/release-notes/android
    implementation(platform("com.google.firebase:firebase-bom:31.3.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    // https://github.com/google/auto/releases
    implementation("com.google.auto.factory:auto-factory:1.0.1")

    // https://developer.android.com/jetpack/androidx/releases/compose-material
    implementation("androidx.compose.material:material:1.4.0")

    // https://developer.android.com/jetpack/androidx/releases/activity
    implementation("androidx.activity:activity-compose:1.7.0")

    // https://github.com/junit-team/junit4/releases
    testImplementation("junit:junit:4.13.2")

    // https://developer.android.com/jetpack/androidx/releases/test
    androidTestImplementation("androidx.test:runner:1.5.2")
}
