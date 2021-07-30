# https://www.guardsquare.com/en/products/proguard/manual/usage

-keepattributes SourceFile, LineNumberTable

-keepnames class io.github.bgavyus.lightningcamera.**

-assumevalues class io.github.bgavyus.lightningcamera.BuildConfig {
    #noinspection SyntaxError
    boolean DEBUG return false;
}
