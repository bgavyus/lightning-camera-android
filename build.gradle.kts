plugins {
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin.get() apply false
    id("org.jetbrains.kotlin.kapt") version libs.versions.kotlin.get() apply false
    id("com.google.dagger.hilt.android") version libs.versions.hilt.get() apply false

    // https://developer.android.com/studio/releases/gradle-plugin
    id("com.android.application") version "8.0.2" apply false

    // https://developers.google.com/android/guides/releases
    id("com.google.gms.google-services") version "4.3.15" apply false

    // https://firebase.google.com/support/release-notes/android
    id("com.google.firebase.crashlytics") version "2.9.5" apply false
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false

    // https://github.com/PrzemyslawSwiderski/python-gradle-plugin/blob/master/CHANGELOG.md
    id("com.pswidersk.python-plugin") version "2.4.0" apply false
}
