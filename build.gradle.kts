buildscript {
    val kotlinVersion by extra("1.4.10")
    val hiltVersion by extra("2.29.1-alpha")

    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.1")
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
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
