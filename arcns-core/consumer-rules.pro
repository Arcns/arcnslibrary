##---------------Begin: proguard configuration for glide----------

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

##---------------Begin: proguard configuration for InjectSuperViewModel----------
-keepattributes Signature
-keepattributes *Annotation*
# 如果类中有使用了注解的成员，则不混淆类和类成员
#-keep class com.arcns.core.util.InjectSuperViewModel
#-keepclasseswithmembers class * {
#    @com.arcns.core.util.InjectSuperViewModel <methods>;
#}
#-keepclasseswithmembers class * {
#    @com.arcns.core.util.InjectSuperViewModel <fields>;
#}
-keep class kotlin.Metadata { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
