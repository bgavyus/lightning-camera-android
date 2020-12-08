plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "29.0.3"

    defaultConfig {
        applicationId = "io.github.bgavyus.lightningcamera"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdkVersion(24)
        targetSdkVersion(30)
        versionCode = 7
        versionName = "0.0.7"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.1")

    val kotlinVersion: String by rootProject.extra
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")

    val hiltVersion: String by rootProject.extra
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")

    val jetpackHiltVersion = "1.0.0-alpha02"
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:$jetpackHiltVersion")
    kapt("androidx.hilt:hilt-compiler:$jetpackHiltVersion")

    implementation("androidx.core:core-ktx:1.5.0-alpha05")
    implementation("androidx.fragment:fragment-ktx:1.3.0-beta01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    // https://github.com/natario1/Egloo/tags
    implementation("com.otaliastudios.opengl:egloo:0.5.3")

    androidTestImplementation("androidx.test:runner:1.3.0")
}
