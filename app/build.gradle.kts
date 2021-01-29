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

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"

    defaultConfig {
        applicationId = "io.github.bgavyus.lightningcamera"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdkVersion(24)
        targetSdkVersion(30)
        versionCode = 10
        versionName = "0.1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName(BuilderConstants.RELEASE) {
            isMinifyEnabled = false

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

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs =
            freeCompilerArgs + "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.1")

    val kotlinVersion: String by rootProject.extra
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // https://github.com/Kotlin/kotlinx.coroutines/releases
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")

    val hiltVersion: String by rootProject.extra
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")

    val jetpackHiltVersion = "1.0.0-alpha03"
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:$jetpackHiltVersion")
    kapt("androidx.hilt:hilt-compiler:$jetpackHiltVersion")

    implementation("androidx.core:core-ktx:1.5.0-beta01")
    implementation("androidx.fragment:fragment-ktx:1.3.0-rc02")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    // https://github.com/natario1/Egloo/releases
    implementation("com.otaliastudios.opengl:egloo:0.5.4")

    // https://firebase.google.com/support/release-notes/android
    implementation(platform("com.google.firebase:firebase-bom:26.4.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    androidTestImplementation("androidx.test:runner:1.3.0")
}
