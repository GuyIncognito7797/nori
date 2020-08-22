# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/22.6.2/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:
-keep public class * extends android.view.View {
        public <init>(android.content.Context);
        public <init>(android.content.Context, android.util.AttributeSet);
        public <init>(android.content.Context, android.util.AttributeSet, int);
        public void set*(...);
}

-keep public class androidx.appcompat.widget.ShareActionProvider {
  public <init>(...);
}

-dontwarn com.koushikdutta.ion.**
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
