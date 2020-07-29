plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(29)
    buildToolsVersion = "29.0.3"

    defaultConfig {
        applicationId = "io.github.bgavyus.splash"
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
    val kotlinVersion: String by rootProject.extra

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.fragment:fragment-ktx:1.2.5")
    implementation("com.otaliastudios.opengl:egloo:0.5.1")
}
