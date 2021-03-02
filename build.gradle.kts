buildscript {
    // https://kotlinlang.org/docs/releases.html
    val kotlinVersion by extra("1.4.30")

    // https://github.com/google/dagger/releases
    val hiltVersion by extra("2.32-alpha")

    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
        classpath("com.google.gms:google-services:4.3.5")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.5.1")
        classpath("com.google.firebase:perf-plugin:1.3.5")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
