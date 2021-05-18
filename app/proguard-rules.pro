# https://www.guardsquare.com/en/products/proguard/manual/usage

-keepattributes SourceFile, LineNumberTable

-keepnames class io.github.bgavyus.lightningcamera.**

-assumevalues class io.github.bgavyus.lightningcamera.BuildConfig {
    boolean DEBUG return false;
}
