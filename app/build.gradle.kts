import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.builder.core.BuilderConstants
import com.pswidersk.gradle.python.VenvTask

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
    id("com.pswidersk.python-plugin")
}

kapt.correctErrorTypes = true

android {
    compileSdk = 34

    // https://developer.android.com/studio/releases/build-tools
    buildToolsVersion = "33.0.2"

    defaultConfig {
        applicationId = "io.github.bgavyus.lightningcamera"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 24
        targetSdk = 34
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

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions.freeCompilerArgs = listOf(
        "-opt-in=kotlinx.coroutines.FlowPreview",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    )

    packagingOptions.resources.excludes += listOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "DebugProbesKt.bin",
    )

    // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
    composeOptions.kotlinCompilerExtensionVersion = "1.5.3"

    testOptions.unitTests.isReturnDefaultValues = true
    namespace = "io.github.bgavyus.lightningcamera"
    androidResources.noCompress.add("tflite")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}")

    // https://github.com/Kotlin/kotlinx.coroutines/releases
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.google.dagger:hilt-android:${libs.versions.hilt.get()}")
    kapt("com.google.dagger:hilt-compiler:${libs.versions.hilt.get()}")

    // https://developers.google.com/android/guides/releases
    implementation("com.google.android.gms:play-services-tflite-java:16.1.0")
    implementation("com.google.android.gms:play-services-tflite-support:16.1.0")

    // https://developer.android.com/jetpack/androidx/releases/core
    implementation("androidx.core:core-ktx:1.13.1")

    // https://developer.android.com/jetpack/androidx/releases/fragment
    implementation("androidx.fragment:fragment-ktx:1.7.0")

    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // https://developer.android.com/jetpack/androidx/releases/constraintlayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // https://natario1.github.io/Egloo/about/changelog
    implementation("com.otaliastudios.opengl:egloo:0.6.1")

    // https://firebase.google.com/support/release-notes/android
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    // https://github.com/google/auto/releases
    implementation("com.google.auto.factory:auto-factory:1.1.0")

    // https://developer.android.com/jetpack/androidx/releases/compose-material
    implementation("androidx.compose.material:material:1.6.7")

    // https://developer.android.com/jetpack/androidx/releases/activity
    implementation("androidx.activity:activity-compose:1.9.0")

    // https://github.com/junit-team/junit4/releases
    testImplementation("junit:junit:4.13.2")

    // https://developer.android.com/jetpack/androidx/releases/test
    androidTestImplementation("androidx.test:runner:1.5.2")
}

pythonPlugin {
    // https://docs.conda.io/projects/miniconda/en/latest/miniconda-release-notes.html
    condaVersion.set("py311_23.9.0-0")
    pythonVersion.set("3.11.5")
}

tasks {
    val modelProjectDirectory = projectDir.resolve("../motion")

    val prepareModelCompiler by registering(VenvTask::class) {
        val requirementsFile = modelProjectDirectory.resolve("requirements.txt")
        inputs.file(requirementsFile)
        outputs.dir(projectDir.resolve(".gradle/python"))
        venvExec = "pip"
        args = listOf("install", "-r", requirementsFile.path)
    }

    val compileModel by registering(VenvTask::class) {
        dependsOn(prepareModelCompiler)
        val modelCompilerFile = modelProjectDirectory.resolve("main.py")
        inputs.file(modelCompilerFile)
        val modelFile = projectDir.resolve("src/main/assets/motion.tflite")
        outputs.file(modelFile)
        venvExec = "python"
        args = listOf(modelCompilerFile.path, modelFile.path)
    }

    preBuild.dependsOn(compileModel)
}
