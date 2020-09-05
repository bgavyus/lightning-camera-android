plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdkVersion(29)
    buildToolsVersion = "29.0.3"

    defaultConfig {
        applicationId = "io.github.bgavyus.splash"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdkVersion(24)
        targetSdkVersion(29)
        versionCode = 4
        versionName = "0.0.4"
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
        coreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = freeCompilerArgs +
            "-Xopt-in=kotlin.time.ExperimentalTime" +
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.0.10")

    val kotlinVersion: String by rootProject.extra
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")

    val hiltVersion: String by rootProject.extra
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")

    val jetpackHiltVersion = "1.0.0-alpha02"
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:$jetpackHiltVersion")
    kapt("androidx.hilt:hilt-compiler:$jetpackHiltVersion")

    implementation("androidx.constraintlayout:constraintlayout:2.0.1")
    implementation("androidx.fragment:fragment-ktx:1.3.0-alpha08")

    // https://github.com/natario1/Egloo/tags
    implementation("com.otaliastudios.opengl:egloo:0.5.3")

    androidTestImplementation("androidx.test:runner:1.3.0")
}
