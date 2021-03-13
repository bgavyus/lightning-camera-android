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

kapt {
    correctErrorTypes = true
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

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        useIR = true
        jvmTarget = JavaVersion.VERSION_1_8.toString()

        freeCompilerArgs = listOf(
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xinline-classes",
            "-language-version", "1.5",
            "-api-version", "1.5"
        )
    }

    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    val kotlinVersion: String by rootProject.extra
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")

    // https://github.com/Kotlin/kotlinx.coroutines/releases
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")

    val hiltVersion: String by rootProject.extra
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")

    // https://developer.android.com/jetpack/androidx/releases/hilt
    val jetpackHiltVersion = "1.0.0-alpha03"
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:$jetpackHiltVersion")
    kapt("androidx.hilt:hilt-compiler:$jetpackHiltVersion")

    // https://developer.android.com/jetpack/androidx/releases/core
    implementation("androidx.core:core-ktx:1.5.0-beta03")

    // https://developer.android.com/jetpack/androidx/releases/fragment
    implementation("androidx.fragment:fragment-ktx:1.3.1")

    // https://developer.android.com/jetpack/androidx/releases/constraintlayout
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    // https://natario1.github.io/Egloo/about/changelog
    implementation("com.otaliastudios.opengl:egloo:0.5.4")

    // https://firebase.google.com/support/release-notes/android
    implementation(platform("com.google.firebase:firebase-bom:26.6.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    // https://github.com/google/auto/releases
    implementation("com.google.auto.factory:auto-factory:1.0-beta8")

    // https://github.com/junit-team/junit4/releases
    testImplementation("junit:junit:4.13.2")

    // https://developer.android.com/jetpack/androidx/releases/test
    androidTestImplementation("androidx.test:runner:1.3.0")
}
