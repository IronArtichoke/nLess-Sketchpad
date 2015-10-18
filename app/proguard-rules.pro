-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-dontwarn com.squareup.okhttp.**

-keep class com.nhaarman.listviewanimations.** { *; }
-dontwarn com.nhaarman.listviewanimations.**

-keep class com.nineoldandroids.animation.**  { *; }
-dontwarn com.nineoldandroids.animation.**

-keep class com.google.common.base.Predicate  { *; }
-dontwarn com.google.common.base.Predicate

-keep class com.wnafee.vector.** { *; }

-keep class org.tukaani.xz.**  { *; }
-dontwarn org.tukaani.xz.**

-dontwarn org.hamcrest.**
-dontwarn junit.**