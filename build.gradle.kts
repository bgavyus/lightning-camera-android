buildscript {
    // https://kotlinlang.org/docs/releases.html
    val kotlinVersion by extra("1.4.31")

    // https://github.com/google/dagger/releases
    val hiltVersion by extra("2.35.1")

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.3")
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
        classpath("com.google.gms:google-services:4.3.5")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.5.2")
        classpath("com.google.firebase:perf-plugin:1.3.5")
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
