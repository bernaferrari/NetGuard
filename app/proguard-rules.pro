# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/marcel/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#Line numbers
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

#NetGuard
-keepnames class com.bernaferari.renetguard.** { *; }

# AppFunctions platform extensions are provided by the OS on Android 16+.
-dontwarn com.android.extensions.appfunctions.**

# Compose Multiplatform resources (instrumented-test helper; not on release classpath).
-dontwarn androidx.test.platform.app.InstrumentationRegistry

#JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

#JNI callbacks
-keep class com.bernaferari.renetguard.Allowed { *; }
-keep class com.bernaferari.renetguard.Packet { *; }
-keep class com.bernaferari.renetguard.ResourceRecord { *; }
-keep class com.bernaferari.renetguard.Usage { *; }
-keep class com.bernaferari.renetguard.ServiceSinkhole {
    void nativeExit(java.lang.String);
    void nativeError(int, java.lang.String);
    void logPacket(com.bernaferari.renetguard.Packet);
    void dnsResolved(com.bernaferari.renetguard.ResourceRecord);
    boolean isDomainBlocked(java.lang.String);
    int getUidQ(int, int, java.lang.String, int, java.lang.String, int);
    com.bernaferari.renetguard.Allowed isAddressAllowed(com.bernaferari.renetguard.Packet);
    void accountUsage(com.bernaferari.renetguard.Usage);
}

#AndroidX

-keepclassmembers class * implements android.os.Parcelable { static ** CREATOR; }



#AdMob
-dontwarn com.google.android.gms.internal.**

# Optional WindowManager extension/sidecar APIs are loaded reflectively.
-dontwarn androidx.window.extensions.WindowExtensions
-dontwarn androidx.window.extensions.WindowExtensionsProvider
-dontwarn androidx.window.extensions.area.ExtensionWindowAreaPresentation
-dontwarn androidx.window.extensions.core.util.function.Consumer
-dontwarn androidx.window.extensions.core.util.function.Function
-dontwarn androidx.window.extensions.core.util.function.Predicate
-dontwarn androidx.window.extensions.layout.DisplayFeature
-dontwarn androidx.window.extensions.layout.FoldingFeature
-dontwarn androidx.window.extensions.layout.WindowLayoutComponent
-dontwarn androidx.window.extensions.layout.WindowLayoutInfo
-dontwarn androidx.window.sidecar.SidecarDeviceState
-dontwarn androidx.window.sidecar.SidecarDisplayFeature
-dontwarn androidx.window.sidecar.SidecarInterface$SidecarCallback
-dontwarn androidx.window.sidecar.SidecarInterface
-dontwarn androidx.window.sidecar.SidecarProvider
-dontwarn androidx.window.sidecar.SidecarWindowLayoutInfo
