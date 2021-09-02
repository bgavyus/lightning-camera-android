buildscript {
    // https://kotlinlang.org/docs/releases.html
    val kotlinVersion by extra("1.5.20")

    // https://github.com/google/dagger/releases
    val hiltVersion by extra("2.38.1")

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        // https://developer.android.com/studio/releases/gradle-plugin
        classpath("com.android.tools.build:gradle:7.0.2")

        // https://developers.google.com/android/guides/releases
        classpath("com.google.gms:google-services:4.3.8")

        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.7.1")
        classpath("com.google.firebase:perf-plugin:1.4.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
