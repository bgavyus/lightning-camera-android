buildscript {
    // https://kotlinlang.org/docs/releases.html
    val kotlinVersion by extra("1.5.0")

    // https://github.com/google/dagger/releases
    val hiltVersion by extra("2.35.1")

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.2.1")
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
        classpath("com.google.gms:google-services:4.3.5")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.6.1")
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
